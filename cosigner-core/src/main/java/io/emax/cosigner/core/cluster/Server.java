package io.emax.cosigner.core.cluster;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Server {
  @JsonProperty
  private String serverLocation;
  @JsonProperty
  private int serverRPCPort;
  @JsonProperty
  private int serverListeningPort;
  @JsonProperty
  private boolean isOriginator;
  @JsonProperty
  private String serverID;
  @JsonProperty
  private long lastCommunication;

  public String getServerLocation() {
    return serverLocation;
  }

  public void setServerLocation(String serverLocation) {
    this.serverLocation = serverLocation;
  }

  public int getServerRPCPort() {
    return serverRPCPort;
  }

  public void setServerRPCPort(int serverRPCPort) {
    this.serverRPCPort = serverRPCPort;
  }

  public int getServerListeningPort() {
    return serverListeningPort;
  }

  public void setServerListeningPort(int serverListeningPort) {
    this.serverListeningPort = serverListeningPort;
  }

  public boolean isOriginator() {
    return isOriginator;
  }

  public void setOriginator(boolean isOriginator) {
    this.isOriginator = isOriginator;
  }

  public String getServerID() {
    return serverID;
  }

  public void setServerID(String serverID) {
    this.serverID = serverID;
  }

  public long getLastCommunication() {
    return lastCommunication;
  }

  public void setLastCommunication(long lastCommunication) {
    this.lastCommunication = lastCommunication;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + serverListeningPort;
    result = prime * result + ((serverLocation == null) ? 0 : serverLocation.hashCode());
    result = prime * result + serverRPCPort;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Server other = (Server) obj;
    if (serverListeningPort != other.serverListeningPort)
      return false;
    if (serverLocation == null) {
      if (other.serverLocation != null)
        return false;
    } else if (!serverLocation.equals(other.serverLocation))
      return false;
    if (serverRPCPort != other.serverRPCPort)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Server [serverLocation=" + serverLocation + ", serverRPCPort=" + serverRPCPort
        + ", serverListeningPort=" + serverListeningPort + ", isOriginator=" + isOriginator
        + ", serverID=" + serverID + ", lastCommunication=" + lastCommunication + "]";
  }

}
