package io.emax.cosigner.core.cluster;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.emax.cosigner.api.core.Server;
import io.emax.cosigner.api.core.ServerStatus;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.crypto.Secp256k1;
import io.emax.cosigner.core.CosignerApplication;
import io.emax.cosigner.core.CosignerConfiguration;
import io.emax.cosigner.core.cluster.commands.ClusterCommand;
import io.emax.cosigner.core.cluster.commands.ClusterCommandType;

import org.bouncycastle.crypto.digests.SHA3Digest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ClusterInfo {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterInfo.class);

  private String clusterKey = "";
  private Set<Server> servers = new HashSet<>();
  private Server thisServer = new Server();

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
    this.thisServer.setServerId(ServerKey.getServerId());
    this.setClusterKey(ByteUtilities.toHexString(Secp256k1.generatePrivateKey()));
    this.thisServer.setCurrencyStatus(new HashMap<String, ServerStatus>());
    CosignerApplication.getCurrencies().forEach((currency, currencyPackage) -> {
      this.thisServer.getCurrencyStatus().put(currency, ServerStatus.UNKNOWN);
    });

    servers.add(thisServer);
  }

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

  public String getClusterKey() {
    return clusterKey;
  }

  /**
   * Sets the cluster key for the node.
   */
  public void setClusterKey(String clusterKey) {
    this.clusterKey = clusterKey;

    // Sign this server with the key.
    String[] signatures = ServerKey.getClusterSignature(clusterKey);
    thisServer.setSigR(signatures[0]);
    thisServer.setSigS(signatures[1]);
    thisServer.setSigV(signatures[2]);

    servers.remove(thisServer);
    servers.add(thisServer);
  }

  public void updateCurrencyStatus() {
    CosignerApplication.getCurrencies().forEach((currency, currencyPackage) -> {
      thisServer.getCurrencyStatus().put(currency, currencyPackage.getWallet().getWalletStatus());
    });

    servers.remove(thisServer);
    servers.add(thisServer);
  }

  /**
   * Add a server discovered via beacon.
   *
   * <p>Servers announced via beacon may not have the most up to date cluster keys. When we get a
   * message for one we need to respond with a signed heartbeat. This will trigger the server to add
   * us if the keys match, and send us its known hosts.
   */
  public void addBeaconServer(Server server) {
    if (!this.getServers().contains(server)) {
      LOGGER.debug("Got beacon for server: " + server);
      LOGGER.debug("Server is new to us, sending it a heartbeat");
      ClusterCommand command = new ClusterCommand();
      command.setCommandType(ClusterCommandType.HEARTBEAT);
      command.getServer().add(getThisServer());

      String response = Coordinator.broadcastCommand(command, server);
      LOGGER.debug(response);
    }
  }

  /**
   * Add a server to our known hosts.
   *
   * <p>Returns true or false depending on whether the server's signature is acceptable or not.
   */
  public boolean addServer(Server server, boolean wasHeartbeat) {
    LOGGER.debug("Attempting to add a server: " + server);
    LOGGER.debug("Is a heartbeat: " + wasHeartbeat);
    if (!getThisServer().equals(server)) {
      server.setOriginator(false);
    }

    byte[] data = ByteUtilities.toByteArray(server.getServerId());
    // Hash the public key
    SHA3Digest sha3 = new SHA3Digest(256);
    sha3.reset();
    sha3.update(data, 0, data.length);
    byte[] hashedBytes = new byte[256 / 8];
    sha3.doFinal(hashedBytes, 0);

    String recoveredPublicKey = ByteUtilities.toHexString(Secp256k1.recoverPublicKey(
        ByteUtilities.toByteArray(server.getSigR()), ByteUtilities.toByteArray(server.getSigS()),
        ByteUtilities.toByteArray(server.getSigV()), hashedBytes));

    String publicKey =
        ByteUtilities.toHexString(Secp256k1.getPublicKey(ByteUtilities.toByteArray(clusterKey)));

    if (!publicKey.equalsIgnoreCase(recoveredPublicKey)) {
      LOGGER.debug("Server doesn't belong to this cluster.");
      return false;
    }

    if (wasHeartbeat) {
      // If it was a hearbeat then remove the old one so that we replace it, and update the time.
      if (this.getServers().contains(server)) {
        this.getServers().remove(server);
      }
      server.setLastCommunication(System.currentTimeMillis());
    } else {
      // Otherwise, write in an unknown time if we don't already have an entry for it.
      server.setLastCommunication(0L);
    }
    this.getServers().add(server);
    return true;
  }
}
