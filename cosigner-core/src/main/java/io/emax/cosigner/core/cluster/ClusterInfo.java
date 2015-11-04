package io.emax.cosigner.core.cluster;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.emax.cosigner.core.CosignerApplication;
import io.emax.cosigner.core.CosignerConfiguration;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
    this.thisServer.setServerRpcPort(config.getClusterRpcPort());
    this.thisServer.setOriginator(true);
    this.thisServer.setServerId(UUID.randomUUID().toString());

    servers.add(thisServer);
  }

  private Set<Server> servers = new HashSet<>();

  private Server thisServer = new Server();

  public Set<Server> getServers() {
    return servers;
  }

  @JsonProperty
  public void setServers(Set<Server> servers) {
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
