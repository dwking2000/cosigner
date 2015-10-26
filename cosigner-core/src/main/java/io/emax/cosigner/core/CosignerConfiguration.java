package io.emax.cosigner.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CosignerConfiguration extends io.dropwizard.Configuration {
  private String clusterLocation = "localhost";
  private int clusterPort = 5555;
  private int clusterRpcPort = 8080;

  @JsonProperty
  public String getClusterLocation() {
    return clusterLocation;
  }

  @JsonProperty
  public void setClusterLocation(String clusterLocation) {
    this.clusterLocation = clusterLocation;
  }

  @JsonProperty
  public int getClusterPort() {
    return clusterPort;
  }

  @JsonProperty
  public void setClusterPort(int clusterPort) {
    this.clusterPort = clusterPort;
  }

  @JsonProperty
  public int getClusterRpcPort() {
    return clusterRpcPort;
  }

  @JsonProperty
  public void setClusterRrpPort(int clusterRpcPort) {
    this.clusterRpcPort = clusterRpcPort;
  }


}
