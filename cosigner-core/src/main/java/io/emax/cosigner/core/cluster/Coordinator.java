package io.emax.cosigner.core.cluster;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.zeromq.ZBeacon;
import org.zeromq.ZBeacon.Listener;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.emax.cosigner.core.CosignerApplication;
import io.emax.cosigner.core.CosignerConfiguration;
import rx.Observable;
import rx.functions.Action1;

public class Coordinator {
  // Static resolver
  private static Coordinator coordinator = new Coordinator();

  public static Coordinator getInstance() {
    return coordinator;
  }
  // End Static resolver, begin actual class.

  private Socket responder;

  private Coordinator() {
    try {
      JsonFactory jsonFact = new JsonFactory();
      ClusterInfo cluster = ClusterInfo.getInstance();
      CosignerConfiguration config = CosignerApplication.getConfig();


      ObjectMapper mapper = new ObjectMapper(jsonFact);
      ObjectWriter writer = mapper.writerFor(Server.class);
      String beaconString = writer.writeValueAsString(cluster.getThisServer());

      ZBeacon beacon = new ZBeacon(config.getClusterLocation(), config.getClusterPort(),
          beaconString.getBytes(), false);
      beacon.setListener(new Listener() {
        @Override
        public void onBeacon(InetAddress arg0, byte[] arg1) {
          try {
            String request = new String(arg1);
            JsonParser jsonParser = jsonFact.createParser(request);
            jsonParser.nextToken();

            Server server = new ObjectMapper().readValue(jsonParser, Server.class);
            server.setLastCommunication(System.currentTimeMillis());

            if (!cluster.getServers().contains(server)) {
              server.setOriginator(false);
              cluster.getServers().add(server);
            } else if (server.isOriginator()) {
              int existingLocation = cluster.getServers().indexOf(server);
              cluster.getServers().get(existingLocation)
                  .setLastCommunication(System.currentTimeMillis());
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });

      beacon.start();

      // Register a ZMQ REP socket
      Context context = ZMQ.context(1);
      responder = context.socket(ZMQ.REP);
      responder.bind("tcp://" + cluster.getThisServer().getServerLocation() + ":"
          + cluster.getThisServer().getServerRPCPort());

      Observable.interval(5, TimeUnit.MILLISECONDS).map(tick -> responder.recvStr())
          .subscribe(new Action1<String>() {
            @Override
            public void call(String commandString) {
              // Try to decode it as one of the known command types

              // CurrencyCommand
              CurrencyCommand command = CurrencyCommand.parseCommandString(commandString);
              if (command != null) {
                responder.send(CurrencyCommand.handleCommand(command));
                return;
              }

              // Catch-all
              responder.send("Invalid command format");
            }
          });

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private final static int REQUEST_TIMEOUT = 1500;

  // Expectation is that Common/etc... will loop through servers and figure out if it supports the
  // given currency.
  public static String broadcastCommand(BaseCommand command, Server server) {
    String commandString = command.toJson();

    Context context = ZMQ.context(1);
    Socket requester = context.socket(ZMQ.REQ);
    requester.connect("tcp://" + server.getServerLocation() + ":" + server.getServerRPCPort());

    requester.send(commandString);

    String reply = command.toJson();
    Poller poller = new Poller(1);
    poller.register(requester, Poller.POLLIN);

    poller.poll(REQUEST_TIMEOUT);

    if (poller.pollin(0)) {
      reply = requester.recvStr();
    } else {
      // TODO Timed out, consider removing server
    }

    requester.close();

    return reply;
  }
}
