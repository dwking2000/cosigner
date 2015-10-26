package io.emax.cosigner.core.cluster;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Server {
  @JsonProperty
  private String serverLocation;
  @JsonProperty
  private int serverRpcPort;
  @JsonProperty
  private int serverListeningPort;
  @JsonProperty
  private boolean isOriginator;
  @JsonProperty
  private String serverId;
  @JsonProperty
  private long lastCommunication;

  public String getServerLocation() {
    return serverLocation;
  }

  public void setServerLocation(String serverLocation) {
    this.serverLocation = serverLocation;
  }

  public int getServerRpcPort() {
    return serverRpcPort;
  }

  public void setServerRpcPort(int serverRpcPort) {
    this.serverRpcPort = serverRpcPort;
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

  public String getServerId() {
    return serverId;
  }

  public void setServerId(String serverId) {
    this.serverId = serverId;
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
    result = prime * result + (isOriginator ? 1231 : 1237);
    result = prime * result + (int) (lastCommunication ^ (lastCommunication >>> 32));
    result = prime * result + ((serverId == null) ? 0 : serverId.hashCode());
    result = prime * result + serverListeningPort;
    result = prime * result + ((serverLocation == null) ? 0 : serverLocation.hashCode());
    result = prime * result + serverRpcPort;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Server other = (Server) obj;
    if (isOriginator != other.isOriginator) {
      return false;
    }
    if (lastCommunication != other.lastCommunication) {
      return false;
    }
    if (serverId == null) {
      if (other.serverId != null) {
        return false;
      }
    } else if (!serverId.equals(other.serverId)) {
      return false;
    }
    if (serverListeningPort != other.serverListeningPort) {
      return false;
    }
    if (serverLocation == null) {
      if (other.serverLocation != null) {
        return false;
      }
    } else if (!serverLocation.equals(other.serverLocation)) {
      return false;
    }
    if (serverRpcPort != other.serverRpcPort) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Server [serverLocation=" + serverLocation + ", serverRPCPort=" + serverRpcPort
        + ", serverListeningPort=" + serverListeningPort + ", isOriginator=" + isOriginator
        + ", serverID=" + serverId + ", lastCommunication=" + lastCommunication + "]";
  }

}
