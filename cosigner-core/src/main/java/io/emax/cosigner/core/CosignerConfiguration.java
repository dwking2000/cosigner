package io.emax.cosigner.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedList;
import java.util.List;

public class CosignerConfiguration extends io.dropwizard.Configuration {
  private String clusterLocation = "localhost";
  private int clusterPort = 5555;
  private int clusterRpcPort = 8080;
  private List<String> enabledCurrencies = new LinkedList<>();
  private boolean enableBeacon = true;
  private String persistenceLocation = "mem:txs";

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
  public void setClusterRpcPort(int clusterRpcPort) {
    this.clusterRpcPort = clusterRpcPort;
  }

  @JsonProperty
  public List<String> getEnabledCurrencies() {
    return enabledCurrencies;
  }

  @JsonProperty
  public void setEnabledCurrencies(List<String> enabledCurrencies) {
    this.enabledCurrencies = enabledCurrencies;
  }

  @JsonProperty
  public boolean enableBeacon() {
    return enableBeacon;
  }

  @JsonProperty
  public String getPersistenceLocation() {
    return persistenceLocation;
  }

  @JsonProperty
  public void setPersistenceLocation(String persistenceLocation) {
    this.persistenceLocation = persistenceLocation;
  }
}
