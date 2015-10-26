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
    boolean serverExists = false;

    if (newServer == null) {
      return Response.serverError().build();
    }

    for (Server knownServer : cluster.getServers()) {
      if (knownServer.getServerLocation().equalsIgnoreCase(newServer.getServerLocation())) {
        serverExists = true;
      }
    }

    if (serverExists) {
      return Response.ok("Node already exists").build();
    } else {
      cluster.getServers().add(newServer);
      return Response.ok(newServer).build();
    }
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
    return Response.ok(cluster).build();
  }
}
