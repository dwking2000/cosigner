package io.emax.heimdal.core.currency;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.atmosphere.cpr.AtmosphereResponse;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.emax.heimdal.api.currency.Monitor;
import io.emax.heimdal.api.currency.SigningType;
import io.emax.heimdal.core.Application;
import io.emax.heimdal.core.cluster.ClusterInfo;
import io.emax.heimdal.core.cluster.Coordinator;
import io.emax.heimdal.core.cluster.CurrencyCommand;
import io.emax.heimdal.core.cluster.CurrencyCommandType;
import io.emax.heimdal.core.cluster.Server;
import rx.Subscription;

public class Common {

  public static CurrencyParameters convertParams(String params) {
    try {
      JsonFactory jsonFact = new JsonFactory();
      JsonParser jsonParser = jsonFact.createParser(params);
      CurrencyParameters currencyParams =
          new ObjectMapper().readValue(jsonParser, CurrencyParameters.class);
      return currencyParams;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  public static Object objectifyString(Class<?> objectType, String str) {
    try {
      JsonFactory jsonFact = new JsonFactory();
      JsonParser jsonParser = jsonFact.createParser(str);
      Object obj = new ObjectMapper().readValue(jsonParser, objectType);
      return obj;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  public static String stringifyObject(Class<?> objectType, Object obj) {
    try {
      JsonFactory jsonFact = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(jsonFact);
      ObjectWriter writer = mapper.writerFor(objectType);
      return writer.writeValueAsString(obj);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return "";
    }
  }

  public static CurrencyPackage lookupCurrency(CurrencyParameters params) {
    if (Application.getCurrencies().containsKey(params.getCurrencySymbol())) {
      return Application.getCurrencies().get(params.getCurrencySymbol());
    } else {
      return null;
    }
  }

  public static String getCurrencies() {
    List<String> currencies = new LinkedList<>();
    Application.getCurrencies().keySet().forEach((currency) -> {
      currencies.add(currency);
    });

    String currencyString = stringifyObject(LinkedList.class, currencies);

    return currencyString;
  }

  public static String getNewAccount(String params) {
    CurrencyParameters currencyParams = convertParams(params);

    String response = "";
    CurrencyPackage currency = lookupCurrency(currencyParams);

    String userAccount = currency.getWallet().createAddress(currencyParams.getUserKey());
    LinkedList<String> accounts = new LinkedList<>();
    accounts.add(userAccount);
    String userMultiAccount =
        currency.getWallet().getMultiSigAddress(accounts, currencyParams.getUserKey());
    response = userMultiAccount;

    return response;
  }

  public static String listAllAccounts(String params) {
    CurrencyParameters currencyParams = convertParams(params);

    String response = "";
    CurrencyPackage currency = lookupCurrency(currencyParams);

    LinkedList<String> accounts = new LinkedList<>();
    currency.getWallet().getAddresses(currencyParams.getUserKey()).forEach(accounts::add);
    response = stringifyObject(LinkedList.class, accounts);

    return response;
  }

  public static String getBalance(String params) {
    CurrencyParameters currencyParams = convertParams(params);

    String response = "";
    CurrencyPackage currency = lookupCurrency(currencyParams);

    BigDecimal balance = new BigDecimal(0);
    if (currencyParams.getAccount() == null || currencyParams.getAccount().isEmpty()) {
      for (String account : currency.getWallet().getAddresses(currencyParams.getUserKey())) {
        balance = balance.add(new BigDecimal(currency.getWallet().getBalance(account)));
      }
    } else {
      for (String account : currencyParams.getAccount()) {
        balance = balance.add(new BigDecimal(currency.getWallet().getBalance(account)));
      }
    }

    response = balance.toPlainString();

    return response;
  }

  private static HashMap<String, Subscription> subscriptions = new HashMap<>();
  private static HashMap<String, Monitor> monitors = new HashMap<>();

  public static String monitorBalance(String params, AtmosphereResponse responseSocket) {
    CurrencyParameters currencyParams = convertParams(params);

    String response = "";
    CurrencyPackage currency = lookupCurrency(currencyParams);

    Monitor monitor = currency.getMonitor().createNewMonitor();

    monitor.addAddresses(currencyParams.getAccount());

    CurrencyParameters returnParms = new CurrencyParameters();
    returnParms.setAmount("0");
    monitor.getBalances().forEach((address, balance) -> {
      returnParms.getAccount().add(address);
      returnParms.setAmount(
          new BigDecimal(returnParms.getAmount()).add(new BigDecimal(balance)).toPlainString());
    });
    response = stringifyObject(CurrencyParameters.class, returnParms);

    // Web socket was passed to us
    if (responseSocket != null) {
      
      if (subscriptions.containsKey(responseSocket.uuid())) {
        subscriptions.get(responseSocket.uuid()).unsubscribe();
        subscriptions.remove(responseSocket.uuid());
      }
      
      if (monitors.containsKey(responseSocket.uuid())) {
        monitors.get(responseSocket.uuid()).destroyMonitor();
        monitors.remove(responseSocket.uuid());
      }
      
      Subscription wsSubscription =
          monitor.getObservableBalances().subscribe((balanceMap) -> {
            balanceMap.forEach((address, balance) -> {
              try {
                CurrencyParameters responseParms = new CurrencyParameters();
                responseParms.setAccount(new LinkedList<String>());
                responseParms.getAccount().add(address);
                responseParms.setAmount(balance);
                responseSocket.write(stringifyObject(CurrencyParameters.class, responseParms));
              } catch (Exception e) {
                if (subscriptions.containsKey(responseSocket.uuid())) {
                  subscriptions.get(responseSocket.uuid()).unsubscribe();
                  subscriptions.remove(responseSocket.uuid());
                }
                if (monitors.containsKey(responseSocket.uuid())) {
                  monitors.get(responseSocket.uuid()).destroyMonitor();
                  monitors.remove(responseSocket.uuid());
                }
                return;
              }
            });
          });
      subscriptions.put(responseSocket.uuid(), wsSubscription);
      monitors.put(responseSocket.uuid(), monitor);      
    } else if (currencyParams.getCallback() != null && !currencyParams.getCallback().isEmpty()) {
      // It's a REST callback
      if (subscriptions.containsKey(currencyParams.getCallback())) {
        subscriptions.get(currencyParams.getCallback()).unsubscribe();
        subscriptions.remove(currencyParams.getCallback());
      }
      if (monitors.containsKey(currencyParams.getCallback())) {
        monitors.get(currencyParams.getCallback()).destroyMonitor();
        monitors.remove(currencyParams.getCallback());
      }

      Subscription rsSubscription =
          monitor.getObservableBalances().subscribe((balanceMap) -> {
            balanceMap.forEach((address, balance) -> {
              try {
                CurrencyParameters responseParms = new CurrencyParameters();
                responseParms.setAccount(new LinkedList<String>());
                responseParms.getAccount().add(address);
                responseParms.setAmount(balance);

                HttpPost httpPost = new HttpPost(currencyParams.getCallback());
                httpPost.addHeader("content-type", "application/json");
                StringEntity entity;
                entity = new StringEntity(stringifyObject(CurrencyParameters.class, responseParms));
                httpPost.setEntity(entity);

                HttpClients.createDefault().execute(httpPost).close();
              } catch (Exception e) {
                if (subscriptions.containsKey(currencyParams.getCallback())) {
                  subscriptions.get(currencyParams.getCallback()).unsubscribe();
                  subscriptions.remove(currencyParams.getCallback());
                }
                if (monitors.containsKey(currencyParams.getCallback())) {
                  monitors.get(currencyParams.getCallback()).destroyMonitor();
                  monitors.remove(currencyParams.getCallback());
                }
                return;
              }
            });
          });
      subscriptions.put(currencyParams.getCallback(), rsSubscription);
      monitors.put(currencyParams.getCallback(), monitor);
    } else {
      // We have no way to respond to the caller other than with this response.
      monitor.destroyMonitor();
    }
    
    return response;
  }

  public static String prepareTransaction(String params) {
    CurrencyParameters currencyParams = convertParams(params);

    String response = "";
    CurrencyPackage currency = lookupCurrency(currencyParams);

    // Create the transaction
    List<String> addresses = new LinkedList<>();
    addresses.addAll(currencyParams.getAccount());
    currencyParams.setTransactionData(currency.getWallet().createTransaction(addresses,
        currencyParams.getReceivingAccount(), new BigDecimal(currencyParams.getAmount())));

    // Authorize it with the user account
    String initalTx = currencyParams.getTransactionData();
    currencyParams.setTransactionData(currency.getWallet().signTransaction(initalTx,
        currencyParams.getAccount().get(0), currencyParams.getUserKey()));

    // If the userKey/address combo don't work then we stop here.
    if (currencyParams.getTransactionData().equalsIgnoreCase(initalTx)) {
      return initalTx;
    }

    // Send it if it's a sign-each and there's more than one signature
    // required (we're at 1/X)
    if (currency.getConfiguration().getMinSignatures() > 1
        && currency.getConfiguration().getSigningType().equals(SigningType.SENDEACH)) {
      submitTransaction(stringifyObject(CurrencyParameters.class, currencyParams));
    }

    response = currencyParams.getTransactionData();
    return response;
  }

  public static String approveTransaction(String params, boolean sendToRemotes) {
    CurrencyParameters currencyParams = convertParams(params);

    String response = "a";
    response = "";
    CurrencyPackage currency = lookupCurrency(currencyParams);

    for (Server server : ClusterInfo.getInstance().getServers()) {
      if (server.isOriginator()) { // It's us, try to sign it locally.
        currencyParams.setTransactionData(currency.getWallet().signTransaction(
            currencyParams.getTransactionData(), currencyParams.getAccount().get(0)));
      } else if (sendToRemotes) {
        CurrencyCommand command = new CurrencyCommand();
        command.setCurrencyParams(currencyParams);
        command.setCommandType(CurrencyCommandType.SIGN);
        command = CurrencyCommand.parseCommandString(Coordinator.BroadcastCommand(command, server));

        if (command != null) {
          String originalTx = currencyParams.getTransactionData();
          currencyParams.setTransactionData(command.getCurrencyParams().getTransactionData());

          // If it's send-each and the remote actually signed it, send it.
          if (!originalTx.equalsIgnoreCase(currencyParams.getTransactionData())
              && currency.getConfiguration().getSigningType().equals(SigningType.SENDEACH)) {
            submitTransaction(stringifyObject(CurrencyParameters.class, currencyParams));
          }
        }
      }
    }
    response = currencyParams.getTransactionData();

    return response;
  }

  public static String submitTransaction(String params) {
    CurrencyParameters currencyParams = convertParams(params);

    String response = "";
    CurrencyPackage currency = lookupCurrency(currencyParams);
    response = currency.getWallet().sendTransaction(currencyParams.getTransactionData());

    return response;
  }

}
