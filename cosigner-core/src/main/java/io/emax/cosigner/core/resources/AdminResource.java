package io.emax.cosigner.core.resources;

import io.emax.cosigner.api.core.CurrencyPackageInterface;
import io.emax.cosigner.api.core.Server;
import io.emax.cosigner.api.currency.CurrencyAdmin;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.core.CosignerApplication;
import io.emax.cosigner.core.cluster.ClusterInfo;
import io.emax.cosigner.core.cluster.Coordinator;
import io.emax.cosigner.core.cluster.commands.ClusterCommand;
import io.emax.cosigner.core.cluster.commands.ClusterCommandType;
import io.emax.cosigner.ethereum.token.CurrencyConfigurations.GenericCurrencyPackage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class AdminResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdminResource.class);

  /**
   * Add a node to the current cluster.
   *
   * @param newServer Server information
   * @return If the server is added, it will return the same data back. Otherwise an appropriate
   * status message is returned.
   */
  @POST
  @Path("/AddNode")
  @Produces(MediaType.APPLICATION_JSON)
  public Response addNode(Server newServer) {
    LOGGER.debug("[AddNode:Request]");
    ClusterInfo cluster = ClusterInfo.getInstance();

    if (newServer == null) {
      return Response.serverError().build();
    }

    cluster.addBeaconServer(newServer);

    return Response.ok().build();

  }

  /**
   * Lists all cosigner nodes already in the cluster.
   *
   * @return Cluster information.
   */
  @GET
  @Path("/ListNodes")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listNodes() {
    LOGGER.debug("[ListNodes:Request]");
    ClusterInfo cluster = ClusterInfo.getInstance();
    return Response.ok(cluster.getServers()).build();
  }

  /**
   * Set the cluster key for the current node.
   */
  @POST
  @Path("/SetClusterKey")
  public Response setClusterKey(String clusterKey) {
    ClusterInfo.getInstance().setClusterKey(clusterKey);
    return Response.ok().build();
  }

  /**
   * Get the current cluster key the node is using.
   */
  @GET
  @Path("/GetClusterKey")
  public Response getClusterKey() {
    return Response.ok(ClusterInfo.getInstance().getClusterKey()).build();
  }

  /**
   * Set the current server's routable IP.
   */
  @POST
  @Path("/SetLocation")
  public Response setLocation(String location) {
    Server server = ClusterInfo.getInstance().getThisServer();
    server.setServerLocation(location);
    ClusterInfo.getInstance().getServers().remove(server);
    ClusterInfo.getInstance().getServers().add(server);

    return Response.ok().build();
  }

  // TODO This should probably be read from the cluster.
  @GET
  @Path("/GetConfigurations")
  public Response getConfigurations() {
    HashMap<String, Map<String, String>> configurations = new HashMap<>();
    CosignerApplication.getCurrencies().forEach((symbol, currencyPackage) -> {
      if (CurrencyAdmin.class.isAssignableFrom(currencyPackage.getWallet().getClass())) {
        configurations
            .put(symbol, ((CurrencyAdmin) currencyPackage.getWallet()).getConfiguration());
      }
    });

    String response = Json.stringifyObject(HashMap.class, configurations);
    return Response.ok(response).build();
  }

  @GET
  @Path("/TransactionsEnabled")
  public Response getTransactionState(String currency) {
    if (CosignerApplication.getCurrencies().containsKey(currency)) {
      CurrencyPackageInterface currencyPackage = CosignerApplication.getCurrencies().get(currency);
      if (CurrencyAdmin.class.isAssignableFrom(currencyPackage.getWallet().getClass())) {
        return Response.ok(((CurrencyAdmin) currencyPackage.getWallet()).transactionsEnabled())
            .build();
      }
    }

    return Response.noContent().build();
  }

  @POST
  @Path("/DisableTransactions")
  public Response disableTransactions(String currency) {
    if (CosignerApplication.getCurrencies().containsKey(currency)) {
      CurrencyPackageInterface currencyPackage = CosignerApplication.getCurrencies().get(currency);
      if (CurrencyAdmin.class.isAssignableFrom(currencyPackage.getWallet().getClass())) {
        ((CurrencyAdmin) currencyPackage.getWallet()).disableTransactions();
        for (Server server : ClusterInfo.getInstance().getServers()) {
          ClusterCommand command = new ClusterCommand();

          // Configure command
          command.setCommandType(ClusterCommandType.DISABLETXS);
          command.setCommandData(currency);
          Coordinator.broadcastCommand(command, server);
        }
        return Response.ok(((CurrencyAdmin) currencyPackage.getWallet()).transactionsEnabled())
            .build();
      }
    }

    return Response.noContent().build();
  }

  @POST
  @Path("/EnableTransactions")
  public Response enableTransactions(String currency) {
    if (CosignerApplication.getCurrencies().containsKey(currency)) {
      CurrencyPackageInterface currencyPackage = CosignerApplication.getCurrencies().get(currency);
      if (CurrencyAdmin.class.isAssignableFrom(currencyPackage.getWallet().getClass())) {
        ((CurrencyAdmin) currencyPackage.getWallet()).enableTransactions();

        for (Server server : ClusterInfo.getInstance().getServers()) {
          ClusterCommand command = new ClusterCommand();

          // Configure command
          command.setCommandType(ClusterCommandType.ENABLETXS);
          command.setCommandData(currency);
          Coordinator.broadcastCommand(command, server);
        }
        return Response.ok(((CurrencyAdmin) currencyPackage.getWallet()).transactionsEnabled())
            .build();
      }
    }

    return Response.noContent().build();
  }

  @POST
  @Path("/LoadEthToken")
  public Response loadEthToken(String currency) {
    if (CosignerApplication.getCurrencies().containsKey(currency)) {
      return Response.noContent().build();
    } else {
      CurrencyPackageInterface currencyPackage = new GenericCurrencyPackage(currency);
      CosignerApplication.getCurrencies()
          .put(currencyPackage.getConfiguration().getCurrencySymbol(), currencyPackage);
      return Response.ok().build();
    }
  }

  @POST
  @Path("/GetBlockchainHeight")
  public Response getBlockchainHeigh(String currency) {
    if (CosignerApplication.getCurrencies().containsKey(currency)) {
      CurrencyPackageInterface currencyPackage = CosignerApplication.getCurrencies().get(currency);
      return Response.ok(((CurrencyAdmin) currencyPackage.getWallet()).getBlockchainHeight())
          .build();
    }

    return Response.noContent().build();
  }

  @POST
  @Path("/GetLastBlockTime")
  public Response getLastBlockTime(String currency) {
    if (CosignerApplication.getCurrencies().containsKey(currency)) {
      CurrencyPackageInterface currencyPackage = CosignerApplication.getCurrencies().get(currency);
      return Response.ok(((CurrencyAdmin) currencyPackage.getWallet()).getLastBlockTime()).build();
    }

    return Response.noContent().build();
  }
}
