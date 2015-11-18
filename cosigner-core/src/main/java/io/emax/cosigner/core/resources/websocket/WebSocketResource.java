package io.emax.cosigner.core.resources.websocket;

import io.emax.cosigner.core.currency.Common;

import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.handler.OnMessage;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import org.atmosphere.interceptor.BroadcastOnPostAtmosphereInterceptor;
import org.atmosphere.interceptor.HeartbeatInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;

import javax.ws.rs.Path;

@Path("/")
@AtmosphereHandlerService(path = "/ws", broadcasterCache = UUIDBroadcasterCache.class,
    interceptors = {AtmosphereResourceLifecycleInterceptor.class,
        BroadcastOnPostAtmosphereInterceptor.class, TrackMessageSizeInterceptor.class,
        HeartbeatInterceptor.class})
public class WebSocketResource extends OnMessage<String> {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketResource.class);

  @Override
  public void onMessage(AtmosphereResponse arg0, String arg1) throws IOException {
    String functionCall = arg0.request().getRequestURI().replaceAll("/ws", "");
    switch (functionCall.toLowerCase(Locale.US)) {
      case "/listcurrencies":
        LOGGER.debug("[GetCurrencies:WSRequest]");
        arg0.write(Common.listCurrencies());
        break;
      case "/registeraddress":
        LOGGER.debug("[GetCurrencies:WSRequest]");
        arg0.write(Common.registerAddress(arg1));
        break;
      case "/getnewaddress":
        LOGGER.debug("[GetNewAccount:WSRequest]");
        arg0.write(Common.getNewAddress(arg1));
        break;
      case "/listalladdresses":
        LOGGER.debug("[ListAllAccounts:WSRequest]");
        arg0.write(Common.listAllAddresses(arg1));
        break;
      case "/listtransactions":
        LOGGER.debug("[ListTransactions:WSRequest]");
        arg0.write(Common.listTransactions(arg1));
        break;
      case "/getbalance":
        LOGGER.debug("[GetBalance:WSRequest]");
        arg0.write(Common.getBalance(arg1));
        break;
      case "/monitorbalance":
        LOGGER.debug("[MonitorBalance:WSRequest]");
        arg0.write(Common.monitorBalance(arg1, arg0));
        break;
      case "/preparetransaction":
        LOGGER.debug("[PrepareTransaction:WSRequest]");
        arg0.write(Common.prepareTransaction(arg1));
        break;
      case "/approvetransaction":
        LOGGER.debug("[ApproveTransaction:WSRequest]");
        arg0.write(Common.approveTransaction(arg1, true));
        break;
      case "/submittransaction":
        LOGGER.debug("[SubmitTransaction:WSRequest]");
        arg0.write(Common.submitTransaction(arg1));
        break;
      default:
        LOGGER.warn("[Invalid:WSRequest] " + arg1);
        arg0.write("No such function");
        break;
    }
  }
}
