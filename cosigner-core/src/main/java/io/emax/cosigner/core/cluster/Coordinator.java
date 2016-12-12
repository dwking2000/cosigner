package io.emax.cosigner.core.cluster;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.emax.cosigner.api.core.Server;
import io.emax.cosigner.core.CosignerApplication;
import io.emax.cosigner.core.CosignerConfiguration;
import io.emax.cosigner.core.cluster.commands.BaseCommand;
import io.emax.cosigner.core.cluster.commands.ClusterCommand;
import io.emax.cosigner.core.cluster.commands.ClusterCommandType;
import io.emax.cosigner.core.cluster.commands.CurrencyCommand;
import io.emax.cosigner.core.cluster.commands.EncryptedCommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZBeacon;
import org.zeromq.ZBeacon.Listener;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

import rx.Observable;
import rx.functions.Action1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class Coordinator {
  private static final Logger LOGGER = LoggerFactory.getLogger(Coordinator.class);
  private static final Coordinator coordinator = new Coordinator();
  private static final int REQUEST_TIMEOUT = 1500;
  private Socket responder;

  public static Coordinator getInstance() {
    return coordinator;
  }

  private Coordinator() {
    try {
      JsonFactory jsonFact = new JsonFactory();
      ClusterInfo cluster = ClusterInfo.getInstance();
      CosignerConfiguration config = CosignerApplication.getConfig();

      ObjectMapper mapper = new ObjectMapper(jsonFact);
      ObjectWriter writer = mapper.writerFor(Server.class);
      String beaconString = writer.writeValueAsString(cluster.getThisServer());

      ZBeacon beacon = new ZBeacon(config.getClusterLocation(), config.getClusterPort(),
          beaconString.getBytes("UTF-8"), false);
      beacon.setListener(new Listener() {
        @Override
        public void onBeacon(InetAddress arg0, byte[] arg1) {
          try {
            String request = new String(arg1, "UTF-8");
            JsonParser jsonParser = jsonFact.createParser(request);
            jsonParser.nextToken();

            Server server = new ObjectMapper().readValue(jsonParser, Server.class);
            cluster.addBeaconServer(server);
          } catch (RuntimeException | IOException e) {
            LOGGER.warn(null, e);
          }
        }
      });

      beacon.start();

      // Register a ZMQ REP socket
      Context context = ZMQ.context(1);
      responder = context.socket(ZMQ.REP);
      // Don't wait if there are no messages.
      responder.setReceiveTimeOut(1);
      responder.bind(
          "tcp://0.0.0.0:" + cluster.getThisServer()
              .getServerRpcPort());

      Observable.interval(50, TimeUnit.MILLISECONDS).map(tick -> responder.recvStr())
          .subscribe(new Action1<String>() {
            @Override
            public void call(String commandString) {
              // Try to decode it as one of the known command types
              if (commandString == null || commandString.isEmpty()) {
                return;
              }
              LOGGER.debug("Got a remote command: " + commandString);

              // Check if it's an encrypted command first.
              EncryptedCommand encryptedCommand =
                  EncryptedCommand.parseCommandString(commandString);
              if (encryptedCommand != null) {
                LOGGER.debug("Command is an EncryptedCommand");
                commandString =
                    EncryptedCommand.handleCommand(ServerKey.getMykey(), encryptedCommand);
                LOGGER.debug("Decrypted to: " + commandString);
              }

              // CurrencyCommand
              CurrencyCommand currencyCommand = CurrencyCommand.parseCommandString(commandString);
              if (currencyCommand != null) {
                LOGGER.debug("Command is a CurrencyCommand");
                responder.send(CurrencyCommand.handleCommand(currencyCommand));
                return;
              }

              ClusterCommand clusterCommand = ClusterCommand.parseCommandString(commandString);
              if (clusterCommand != null) {
                LOGGER.debug("Command is a ClusterCommand");
                responder.send(Boolean.toString(ClusterCommand.handleCommand(clusterCommand)));
                return;
              }

              // Catch-all
              LOGGER.debug("Command isn't valid.");
              responder.send("Invalid command format");
            }
          });

      // Setup the hearbeat cycle
      Observable.interval(30, TimeUnit.SECONDS).map(tick -> true).subscribe(new Action1<Boolean>() {
        @Override
        public void call(Boolean arg0) {
          LOGGER.debug("Heartbeat tick");
          ClusterInfo.getInstance().getServers().forEach(server -> {
            if (server.isOriginator()) {
              // Skip ourselves.
              return;
            }
            ClusterCommand command = new ClusterCommand();
            command.setCommandType(ClusterCommandType.HEARTBEAT);
            command.getServer().add(ClusterInfo.getInstance().getThisServer());
            LOGGER.debug("Sending heartbeat to: " + server);
            String response = broadcastCommand(command, server);
            LOGGER.debug("Response: " + response);
          });

          cluster.updateCurrencyStatus();
        }

      });

    } catch (IOException e) {
      LOGGER.warn(null, e);
    }
  }

  /**
   * Broadcast a command to a remote server.
   *
   * <p>Example: Send a signature command to the alternate cosigner server. It will respond with
   * signed data.
   *
   * @param command Command to broadcast.
   * @param server  Server to attempt to send the command to.
   * @return Reply from the server.
   */
  public static String broadcastCommand(BaseCommand command, Server server) {
    // Update the comm time if we've actually been talking to this server.
    if (ClusterInfo.getInstance().getServers().contains(server)) {
      ClusterInfo.getInstance().getServers().forEach(trackedServer -> {
        if (trackedServer.equals(server)) {
          server.setLastCommunication(trackedServer.getLastCommunication());
        }
      });
    }

    // If we haven't heard from the server in more then 2 minutes, consider it offline.
    if ((System.currentTimeMillis() - server.getLastCommunication()) > 2 * 60 * 1000) {
      if (command.getClass() == ClusterCommand.class
          && ((ClusterCommand) command).getCommandType() == ClusterCommandType.HEARTBEAT) {
        LOGGER.debug("Server is too old, sending heartbeat");
      } else {
        LOGGER.debug("Server is too old, removing server");
        ClusterInfo.getInstance().getServers().remove(server);
        return "";
      }
    }

    // If it's not a ClusterCommand, encrypt it. ClusterCommands only identify servers, and
    // they're checked for signatures.
    if (command.getClass() != ClusterCommand.class) {
      command =
          new EncryptedCommand(ClusterInfo.getInstance().getThisServer(), ServerKey.getMykey(),
              server, command.toJson());
    }

    String commandString = command.toJson();

    Context context = ZMQ.context(1);
    Socket requester = context.socket(ZMQ.REQ);
    requester.connect("tcp://" + server.getServerLocation() + ":" + server.getServerRpcPort());

    requester.send(commandString);
    LOGGER.debug("Command is in flight");

    String reply = command.toJson();
    Poller poller = new Poller(1);
    poller.register(requester, Poller.POLLIN);

    poller.poll(REQUEST_TIMEOUT);

    if (poller.pollin(0)) {
      reply = requester.recvStr();
      LOGGER.debug("Got response");
    }

    requester.close();

    return reply;
  }
}
