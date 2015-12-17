package io.emax.cosigner.core.resources;

import io.emax.cosigner.api.core.CurrencyParameters;
import io.emax.cosigner.core.currency.Common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/rs")
public class CurrencyResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(CurrencyResource.class);

  /**
   * REST end-point for the {@link Common} currency methods.
   *
   * @return Common.listCurrencies()
   */
  @GET
  @Path("/ListCurrencies")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCurrencies() {
    LOGGER.debug("[GetCurrencies:Request]");
    Response response = Response.ok(Common.listCurrencies()).build();
    LOGGER.debug("[GetCurrencies:Response] " + response.toString());
    return response;
  }

  /**
   * REST end-point for the {@link Common} currency methods.
   *
   * @return Common.registerAddress()
   */
  @POST
  @Path("/RegisterAddress")
  @Produces(MediaType.APPLICATION_JSON)
  public Response registerAddress(String params) {
    LOGGER.debug("[RegisterAddress:Request]");
    Response response = Response.ok(Common.registerAddress(params)).build();
    LOGGER.debug("[RegisterAddress:Response] " + response.toString());
    return response;
  }

  /**
   * REST end-point for the {@link Common} currency methods.
   *
   * @param params JSON representation of {@link CurrencyParameters}
   * @return Common.getNewAddress()
   */
  @POST
  @Path("/GetNewAddress")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getNewAccount(String params) {
    LOGGER.debug("[GetNewAccount:Request]");
    Response response = Response.ok(Common.getNewAddress(params)).build();
    LOGGER.debug("[GetNewAccount:Response] " + response.toString());
    return response;
  }

  /**
   * REST end-point for the {@link Common} currency methods.
   *
   * @param params JSON representation of {@link CurrencyParameters}
   * @return Common.listAllAddresses()
   */
  @POST
  @Path("/ListAllAddresses")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listAllAccounts(String params) {
    LOGGER.debug("[ListAllAccounts:Request]");
    Response response = Response.ok(Common.listAllAddresses(params)).build();
    LOGGER.debug("[ListAllAccounts:Response] " + response.toString());
    return response;
  }

  /**
   * REST end-point for the {@link Common} currency methods.
   *
   * @param params JSON representation of {@link CurrencyParameters}
   * @return Common.listTransactions()
   */
  @POST
  @Path("/ListTransactions")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listTransactions(String params) {
    LOGGER.debug("[ListTransactions:Request]");
    Response response = Response.ok(Common.listTransactions(params)).build();
    LOGGER.debug("[ListTransactions:Response] " + response.toString());
    return response;
  }

  /**
   * REST end-point for the {@link Common} currency methods.
   *
   * @param params JSON representation of {@link CurrencyParameters}
   * @return Common.getBalance()
   */
  @POST
  @Path("/GetBalance")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getBalance(String params) {
    LOGGER.debug("[GetBalance:Request]");
    Response response = Response.ok(Common.getBalance(params)).build();
    LOGGER.debug("[GetBalance:Response] " + response.toString());
    return response;
  }

  /**
   * REST end-point for the {@link Common} currency methods.
   *
   * @param params JSON representation of {@link CurrencyParameters}
   * @return Common.monitorBalance()
   */
  @POST
  @Path("/MonitorBalance")
  @Produces(MediaType.APPLICATION_JSON)
  public Response monitorBalance(String params) {
    LOGGER.debug("[MonitorBalance:Request]");
    Response response = Response.ok(Common.monitorBalance(params, null)).build();
    LOGGER.debug("[MonitorBalance:Response] " + response.toString());
    return response;
  }

  /**
   * REST end-point for the {@link Common} currency methods.
   *
   * @param params JSON representation of {@link CurrencyParameters}
   * @return Common.prepareTransaction()
   */
  @POST
  @Path("/PrepareTransaction")
  @Produces(MediaType.APPLICATION_JSON)
  public Response prepareTransaction(String params) {
    LOGGER.debug("[PrepareTransaction:Request]");
    Response response = Response.ok(Common.prepareTransaction(params)).build();
    LOGGER.debug("[PrepareTransaction:Response] " + response.toString());
    return response;
  }

  /**
   * Get signature data needed to sign an offline transaction.
   */
  @POST
  @Path("/GetSignatureString")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSignatureString(String params) {
    LOGGER.debug("[GetSignatureString:Request]");
    Response response = Response.ok(Common.getSignatureString(params)).build();
    LOGGER.debug("[GetSignatureString:Response] " + response.toString());
    return response;
  }

  /**
   * Apply an offline signature to a transaction.
   */
  @POST
  @Path("/ApplySignature")
  @Produces(MediaType.APPLICATION_JSON)
  public Response applySignature(String params) {
    LOGGER.debug("[ApplySignature:Request]");
    Response response = Response.ok(Common.applySignature(params)).build();
    LOGGER.debug("[ApplySignature:Response] " + response.toString());
    return response;
  }

  /**
   * REST end-point for the {@link Common} currency methods.
   *
   * @param params JSON representation of {@link CurrencyParameters}
   * @return Common.approveTransaction()
   */
  @POST
  @Path("/ApproveTransaction")
  @Produces(MediaType.APPLICATION_JSON)
  public Response approveTransaction(String params) {
    LOGGER.debug("[ApproveTransaction:Request]");
    Response response = Response.ok(Common.approveTransaction(params, true)).build();
    LOGGER.debug("[ApproveTransaction:Response] " + response.toString());
    return response;
  }

  /**
   * REST end-point for the {@link Common} currency methods.
   *
   * @param params JSON representation of {@link CurrencyParameters}
   * @return Common.submitTransaction()
   */
  @POST
  @Path("/SubmitTransaction")
  @Produces(MediaType.APPLICATION_JSON)
  public Response submitTransaction(String params) {
    LOGGER.debug("[SubmitTransaction:Request]");
    Response response = Response.ok(Common.submitTransaction(params)).build();
    LOGGER.debug("[SubmitTransaction:Response] " + response.toString());
    return response;
  }
}
