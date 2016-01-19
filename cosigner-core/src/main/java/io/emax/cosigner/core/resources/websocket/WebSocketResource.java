package io.emax.cosigner.core.resources.websocket;

import io.emax.cosigner.core.currency.Common;

import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;

import javax.ws.rs.Path;

@Path("/")
public class WebSocketResource extends WebSocketAdapter {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketResource.class);

  @Override
  public void onWebSocketText(String arg1) {
    try {
      String functionCall =
          getSession().getUpgradeRequest().getRequestURI().getPath().replaceAll("/ws", "");
      LOGGER.debug("Got URI: " + functionCall);
      switch (functionCall.toLowerCase(Locale.US)) {
        case "/listcurrencies":
          LOGGER.debug("[GetCurrencies:WSRequest]");
          getRemote().sendString(Common.listCurrencies());
          break;
        case "/registeraddress":
          LOGGER.debug("[GetCurrencies:WSRequest]");
          getRemote().sendString(Common.registerAddress(arg1));
          break;
        case "/getnewaddress":
          LOGGER.debug("[GetNewAccount:WSRequest]");
          getRemote().sendString(Common.getNewAddress(arg1));
          break;
        case "/generateaddressfromkey":
          LOGGER.debug("[GenerateAddressFromKey:WSRequest]");
          getRemote().sendString(Common.generateAddressFromKey(arg1));
          break;
        case "/listalladdresses":
          LOGGER.debug("[ListAllAccounts:WSRequest]");
          getRemote().sendString(Common.listAllAddresses(arg1));
          break;
        case "/listtransactions":
          LOGGER.debug("[ListTransactions:WSRequest]");
          getRemote().sendString(Common.listTransactions(arg1));
          break;
        case "/getbalance":
          LOGGER.debug("[GetBalance:WSRequest]");
          getRemote().sendString(Common.getBalance(arg1));
          break;
        case "/monitorbalance":
          LOGGER.debug("[MonitorBalance:WSRequest]");
          getRemote().sendString(Common.monitorBalance(arg1, getSession()));
          break;
        case "/preparetransaction":
          LOGGER.debug("[PrepareTransaction:WSRequest]");
          getRemote().sendString(Common.prepareTransaction(arg1));
          break;
        case "/getsignaturestring":
          LOGGER.debug("[GetSignatureString:WSRequest]");
          getRemote().sendString(Common.getSignatureString(arg1));
          break;
        case "/applysignature":
          LOGGER.debug("[ApplySignature:WSRequest]");
          getRemote().sendString(Common.applySignature(arg1));
          break;
        case "/approvetransaction":
          LOGGER.debug("[ApproveTransaction:WSRequest]");
          getRemote().sendString(Common.approveTransaction(arg1, true));
          break;
        case "/submittransaction":
          LOGGER.debug("[SubmitTransaction:WSRequest]");
          getRemote().sendString(Common.submitTransaction(arg1));
          break;
        default:
          LOGGER.warn("[Invalid:WSRequest] " + arg1);
          getRemote().sendString("No such function");
          break;
      }
      getRemote().flush();
    } catch (IOException e) {
      LOGGER.debug(null, e);
    }
  }
}
