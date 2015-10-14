package io.emax.heimdal.core.resources;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.emax.heimdal.core.currency.Common;

@Path("/rs")
public class CurrencyResource {
  Logger logger = LoggerFactory.getLogger(CurrencyResource.class);

  @GET
  @Path("/ListCurrencies")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCurrencies() {
    logger.debug("[GetCurrencies:Request]");
    Response response = Response.ok(Common.listCurrencies()).build();
    logger.debug("[GetCurrencies:Response] " + response.toString());
    return response;
  }

  @POST
  @Path("/GetNewAddress")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getNewAccount(String params) {
    logger.debug("[GetNewAccount:Request]");
    Response response = Response.ok(Common.getNewAddress(params)).build();
    logger.debug("[GetNewAccount:Response] " + response.toString());
    return response;
  }

  @POST
  @Path("/ListAllAddresses")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listAllAccounts(String params) {
    logger.debug("[ListAllAccounts:Request]");
    Response response = Response.ok(Common.listAllAddresses(params)).build();
    logger.debug("[ListAllAccounts:Response] " + response.toString());
    return response;
  }

  @POST
  @Path("/ListTransactions")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listTransactions(String params) {
    logger.debug("[ListTransactions:Request]");
    Response response = Response.ok(Common.listTransactions(params)).build();
    logger.debug("[ListTransactions:Response] " + response.toString());
    return response;
  }

  @POST
  @Path("/GetBalance")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getBalance(String params) {
    logger.debug("[GetBalance:Request]");
    Response response = Response.ok(Common.getBalance(params)).build();
    logger.debug("[GetBalance:Response] " + response.toString());
    return response;
  }

  @POST
  @Path("/MonitorBalance")
  @Produces(MediaType.APPLICATION_JSON)
  public Response monitorBalance(String params) {
    logger.debug("[MonitorBalance:Request]");
    Response response = Response.ok(Common.monitorBalance(params, null)).build();
    logger.debug("[MonitorBalance:Response] " + response.toString());
    return response;
  }

  @POST
  @Path("/PrepareTransaction")
  @Produces(MediaType.APPLICATION_JSON)
  public Response prepareTransaction(String params) {
    logger.debug("[PrepareTransaction:Request]");
    Response response = Response.ok(Common.prepareTransaction(params)).build();
    logger.debug("[PrepareTransaction:Response] " + response.toString());
    return response;
  }

  @POST
  @Path("/ApproveTransaction")
  @Produces(MediaType.APPLICATION_JSON)
  public Response approveTransaction(String params) {
    logger.debug("[ApproveTransaction:Request]");
    Response response = Response.ok(Common.approveTransaction(params, true)).build();
    logger.debug("[ApproveTransaction:Response] " + response.toString());
    return response;
  }

  @POST
  @Path("/SubmitTransaction")
  @Produces(MediaType.APPLICATION_JSON)
  public Response submitTransaction(String params) {
    logger.debug("[SubmitTransaction:Request]");
    Response response = Response.ok(Common.submitTransaction(params)).build();
    logger.debug("[SubmitTransaction:Response] " + response.toString());
    return response;
  }
}
