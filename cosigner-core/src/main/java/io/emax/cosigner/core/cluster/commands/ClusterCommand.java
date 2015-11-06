package io.emax.cosigner.core.cluster.commands;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.emax.cosigner.core.cluster.ClusterInfo;
import io.emax.cosigner.core.cluster.Coordinator;
import io.emax.cosigner.core.cluster.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

public class ClusterCommand implements BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(ClusterCommand.class);
  private ClusterCommandType commandType;
  private Set<Server> server = new HashSet<>();

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

  @Override
  public String toJson() {
    try {
      JsonFactory jsonFact = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(jsonFact);
      ObjectWriter writer = mapper.writerFor(ClusterCommand.class);
      return writer.writeValueAsString(this);
    } catch (IOException e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      logger.warn(errors.toString());
      return "";
    }
  }

  @Override
  public String toString() {
    return "ClusterCommand [commandType=" + commandType + ", server=" + server + "]";
  }

  /**
   * Parse a JSON string that represents a ClusterCommand..
   */
  public static ClusterCommand parseCommandString(String commandString) {
    try {
      JsonFactory jsonFact = new JsonFactory();
      JsonParser jsonParser = jsonFact.createParser(commandString);
      ClusterCommand clusterCommand =
          new ObjectMapper().readValue(jsonParser, ClusterCommand.class);
      return clusterCommand;
    } catch (IOException e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      logger.warn(errors.toString());
      return null;
    }
  }

  /**
   * Attempt to handle the request in a cluster command.
   */
  public static boolean handleCommand(ClusterCommand command) {
    switch (command.commandType) {
      case Heartbeat:
        logger.debug("Got heartbeat for: " + command);
        command.getServer().forEach(server -> {
          if (ClusterInfo.getInstance().addServer(server, true)) {
            ClusterCommand response = new ClusterCommand();
            response.setCommandType(ClusterCommandType.KnownServers);
            response.getServer().addAll(ClusterInfo.getInstance().getServers());
            String commandResponse = Coordinator.broadcastCommand(response, server);
            logger.debug(commandResponse);
          }
        });
        return true;
      case KnownServers:
        logger.debug("Got list of known servers: " + command);
        command.getServer().forEach(server -> {
          ClusterInfo.getInstance().addServer(server, server.isOriginator());
        });
        return true;
      default:
        return false;
    }
  }

}
