package io.emax.cosigner.core.cluster.commands;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.emax.cosigner.api.core.CurrencyPackageInterface;
import io.emax.cosigner.api.core.Server;
import io.emax.cosigner.api.currency.CurrencyAdmin;
import io.emax.cosigner.core.CosignerApplication;
import io.emax.cosigner.core.cluster.ClusterInfo;
import io.emax.cosigner.core.cluster.Coordinator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ClusterCommand implements BaseCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCommand.class);
  private ClusterCommandType commandType;
  private Set<Server> server = new HashSet<>();
  private String commandData;

  public ClusterCommandType getCommandType() {
    return commandType;
  }

  public void setCommandType(ClusterCommandType commandType) {
    this.commandType = commandType;
  }

  public Set<Server> getServer() {
    return server;
  }

  public void setServer(Set<Server> server) {
    this.server = server;
  }

  public String getCommandData() {
    return commandData;
  }

  public void setCommandData(String commandData) {
    this.commandData = commandData;
  }

  @Override
  public String toJson() {
    try {
      JsonFactory jsonFact = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(jsonFact);
      ObjectWriter writer = mapper.writerFor(ClusterCommand.class);
      return writer.writeValueAsString(this);
    } catch (IOException e) {
      LOGGER.warn(null, e);
      return "";
    }
  }

  @Override
  public String toString() {
    return "ClusterCommand{" + "commandType=" + commandType + ", server=" + server
        + ", commandData='" + commandData + '\'' + '}';
  }

  /**
   * Parse a JSON string that represents a ClusterCommand..
   */
  public static ClusterCommand parseCommandString(String commandString) {
    try {
      JsonFactory jsonFact = new JsonFactory();
      JsonParser jsonParser = jsonFact.createParser(commandString);
      return new ObjectMapper().readValue(jsonParser, ClusterCommand.class);
    } catch (IOException e) {
      LOGGER.debug("Problem parsing Cluster Command, may be another type.");
      LOGGER.trace(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Attempt to handle the request in a cluster command.
   */
  public static boolean handleCommand(ClusterCommand command) {
    switch (command.commandType) {
      case HEARTBEAT:
        LOGGER.debug("Got heartbeat for: " + command);
        command.getServer().forEach(server -> {
          if (ClusterInfo.getInstance().addServer(server, true)) {
            ClusterCommand response = new ClusterCommand();
            response.setCommandType(ClusterCommandType.KNOWNSERVERS);
            response.getServer().addAll(ClusterInfo.getInstance().getServers());
            String commandResponse = Coordinator.broadcastCommand(response, server);
            LOGGER.debug(commandResponse);
          }
        });
        return true;
      case KNOWNSERVERS:
        LOGGER.debug("Got list of known servers: " + command);
        command.getServer()
            .forEach(server -> ClusterInfo.getInstance().addServer(server, server.isOriginator()));
        return true;
      case DISABLETXS:
        if (CosignerApplication.getCurrencies().containsKey(command.getCommandData())) {
          CurrencyPackageInterface currencyPackage =
              CosignerApplication.getCurrencies().get(command.getCommandData());
          if (CurrencyAdmin.class.isAssignableFrom(currencyPackage.getWallet().getClass())) {
            ((CurrencyAdmin) currencyPackage.getWallet()).disableTransactions();
          }
        }
        return true;
      case ENABLETXS:
        if (CosignerApplication.getCurrencies().containsKey(command.getCommandData())) {
          CurrencyPackageInterface currencyPackage =
              CosignerApplication.getCurrencies().get(command.getCommandData());
          if (CurrencyAdmin.class.isAssignableFrom(currencyPackage.getWallet().getClass())) {
            ((CurrencyAdmin) currencyPackage.getWallet()).enableTransactions();
          }
        }
        return true;
      default:
        return false;
    }
  }

}
