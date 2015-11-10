package io.emax.cosigner.core.resources;

import io.emax.cosigner.core.cluster.ClusterInfo;
import io.emax.cosigner.core.cluster.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

// TODO Create an admin utility that uses a second JKS to manage cluster admin vs. client library.
// Admin should be able to change Server settings that are safe to change like:
// address clients can reach it
// cluster key
// manually add a node
// Maybe temporarily disable a currency? vs. full disable like in the config. -- Only thing not done
// at the moment.
// Needs to be able to see the cluster status
@Path("/admin")
public class AdminResource {
  Logger logger = LoggerFactory.getLogger(AdminResource.class);

  /**
   * Add a node to the current cluster.
   * 
   * @param newServer Server information
   * @return If the server is added, it will return the same data back. Otherwise an appropriate
   *         status message is returned.
   */
  @POST
  @Path("/AddNode")
  @Produces(MediaType.APPLICATION_JSON)
  public Response addNode(Server newServer) {
    logger.debug("[AddNode:Request]");
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
    logger.debug("[ListNodes:Request]");
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
}
