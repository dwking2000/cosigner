package io.emax.cosigner.core.cluster;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.emax.cosigner.core.CosignerApplication;
import io.emax.cosigner.core.CosignerConfiguration;

public class ClusterInfo {
  // Static resolver
  private static ClusterInfo clusterInfo = new ClusterInfo();

  public static ClusterInfo getInstance() {
    return clusterInfo;
  }
  // End Static resolver, begin actual class.

  private ClusterInfo() {
    CosignerConfiguration config = CosignerApplication.getConfig();
    this.thisServer.setServerLocation(config.getClusterLocation());
    this.thisServer.setServerListeningPort(config.getClusterPort());
    this.thisServer.setServerRPCPort(config.getClusterRPCPort());
    this.thisServer.setOriginator(true);
    this.thisServer.setServerID(UUID.randomUUID().toString());

    servers.add(thisServer);
  }

  private List<Server> servers = new LinkedList<>();

  private Server thisServer = new Server();

  public List<Server> getServers() {
    return servers;
  }

  @JsonProperty
  public void setServers(List<Server> servers) {
    this.servers = servers;
  }

  public Server getThisServer() {
    return thisServer;
  }

  @JsonProperty
  public void setThisServer(Server thisServer) {
    this.thisServer = thisServer;
  }


}
