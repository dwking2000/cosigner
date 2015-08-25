package io.emax.heimdal.core.resources;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.emax.heimdal.core.currency.Common;

@Path("/rs")
public class CurrencyResource {
  @GET
  @Path("/GetCurrencies")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCurrencies() {
    return Response.ok(Common.getCurrencies()).build();
  }

  @POST
  @Path("/GetNewAccount")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getNewAccount(String params) {
    return Response.ok(Common.getNewAccount(params)).build();
  }

  @POST
  @Path("/ListAllAccounts")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listAllAccounts(String params) {
    return Response.ok(Common.listAllAccounts(params)).build();
  }

  @POST
  @Path("/GetBalance")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getBalance(String params) {
    return Response.ok(Common.getBalance(params)).build();
  }

  @POST
  @Path("/MonitorBalance")
  @Produces(MediaType.APPLICATION_JSON)
  public Response monitorBalance(String params) {
    return Response.ok(Common.monitorBalance(params, null)).build();
  }

  @POST
  @Path("/PrepareTransaction")
  @Produces(MediaType.APPLICATION_JSON)
  public Response prepareTransaction(String params) {
    return Response.ok(Common.prepareTransaction(params)).build();
  }

  @POST
  @Path("/ApproveTransaction")
  @Produces(MediaType.APPLICATION_JSON)
  public Response approveTransaction(String params) {
    return Response.ok(Common.approveTransaction(params, true)).build();
  }

  @POST
  @Path("/SubmitTransaction")
  @Produces(MediaType.APPLICATION_JSON)
  public Response submitTransaction(String params) {
    return Response.ok(Common.submitTransaction(params)).build();
  }
}
