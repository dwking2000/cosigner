package io.emax.cosigner.core.currency;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.emax.cosigner.api.core.CosignerResponse;
import io.emax.cosigner.api.core.CurrencyPackageInterface;
import io.emax.cosigner.api.core.CurrencyParameters;
import io.emax.cosigner.api.core.CurrencyParametersRecipient;
import io.emax.cosigner.api.core.Server;
import io.emax.cosigner.api.currency.Monitor;
import io.emax.cosigner.api.currency.SigningType;
import io.emax.cosigner.api.currency.Wallet.Recipient;
import io.emax.cosigner.api.currency.Wallet.TransactionDetails;
import io.emax.cosigner.api.validation.Validator;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.core.CosignerApplication;
import io.emax.cosigner.core.cluster.ClusterInfo;
import io.emax.cosigner.core.cluster.Coordinator;
import io.emax.cosigner.core.cluster.commands.CurrencyCommand;
import io.emax.cosigner.core.cluster.commands.CurrencyCommandType;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Subscription;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Common {
  private static final Logger LOGGER = LoggerFactory.getLogger(Common.class);

  private static final HashMap<String, HashMap<String, Subscription>> balanceSubscriptions =
      new HashMap<>();
  private static final HashMap<String, HashMap<String, Subscription>> transactionSubscriptions =
      new HashMap<>();
  private static final HashMap<String, HashMap<String, Monitor>> monitors = new HashMap<>();

  private static CurrencyParameters convertParams(String params) {
    try {
      JsonFactory jsonFact = new JsonFactory();
      JsonParser jsonParser = jsonFact.createParser(params);
      CurrencyParameters currencyParams =
          new ObjectMapper().readValue(jsonParser, CurrencyParameters.class);

      String userKey = currencyParams.getUserKey();
      // This is important, keep it out of the logs.
      currencyParams.setUserKey("");
      String sanitizedParams = Json.stringifyObject(CurrencyParameters.class, currencyParams);
      currencyParams.setUserKey(userKey);

      LOGGER.debug("[CurrencyParams] " + sanitizedParams);

      return currencyParams;
    } catch (IOException e) {
      LOGGER.warn(null, e);
      return null;
    }
  }

  /**
   * Lookup the currency package declared in the parameters.
   */
  public static CurrencyPackageInterface lookupCurrency(CurrencyParameters params) {
    if (CosignerApplication.getCurrencies().containsKey(params.getCurrencySymbol())) {
      return CosignerApplication.getCurrencies().get(params.getCurrencySymbol());
    } else {
      return null;
    }
  }

  /**
   * List all currencies that are currently loaded in cosigner.
   *
   * @return String list of currencies.
   */
  public static String listCurrencies() {
    try {
      List<String> currencies = new LinkedList<>();
      CosignerApplication.getCurrencies().keySet().forEach(currencies::add);

      String currencyString = Json.stringifyObject(LinkedList.class, currencies);

      LOGGER.debug("[Response] " + currencyString);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(currencyString);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  /**
   * Registers addresses for currency libraries that need a watch list.
   */
  public static String registerAddress(String params) {
    try {
      CurrencyParameters currencyParams = convertParams(params);
      CurrencyPackageInterface currency = lookupCurrency(currencyParams);

      HashMap<String, Boolean> responses = new HashMap<>();
      currencyParams.getAccount().forEach(address -> {
        Boolean result = currency.getWallet().registerAddress(address);
        responses.put(address, result);
      });

      String response = Json.stringifyObject(HashMap.class, responses);
      LOGGER.debug("[Response] " + response);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(response);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  /**
   * Get a new address for the provided currency & user key.
   *
   * @param params {@link CurrencyParameters} with the currency code and user key filled in.
   * @return Address that the user can use to deposit funds, for which we can generate the private
   * keys.
   */
  public static String getNewAddress(String params) {
    try {
      CurrencyParameters currencyParams = convertParams(params);
      CurrencyPackageInterface currency = lookupCurrency(currencyParams);

      String userAccount = currency.getWallet().createAddress(currencyParams.getUserKey());
      LinkedList<String> accounts = new LinkedList<>();
      if (currencyParams.getAccount() != null) {
        accounts.addAll(currencyParams.getAccount());
      }
      accounts.add(userAccount);

      String response =
          currency.getWallet().getMultiSigAddress(accounts, currencyParams.getUserKey());
      LOGGER.debug("[Response] " + response);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(response);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  /**
   * Generates a currency-specific address from a public key.
   */
  public static String generateAddressFromKey(String params) {
    try {
      CurrencyParameters currencyParams = convertParams(params);
      CurrencyPackageInterface currency = lookupCurrency(currencyParams);

      String publicKey = currencyParams.getUserKey();
      String publicAddress = currency.getWallet().createAddressFromKey(publicKey, false);

      LOGGER.debug("[Response] " + publicAddress);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(publicAddress);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  /**
   * List all addresses that we have generated for the given user key and currency.
   *
   * @param params {@link CurrencyParameters} with the currency code and user key filled in.
   * @return All addresses that cosigner can generate the private key for belonging to that user
   * key.
   */
  public static String listAllAddresses(String params) {
    try {
      CurrencyParameters currencyParams = convertParams(params);
      CurrencyPackageInterface currency = lookupCurrency(currencyParams);

      LinkedList<String> accounts = new LinkedList<>();
      currency.getWallet().getAddresses(currencyParams.getUserKey()).forEach(accounts::add);
      String response = Json.stringifyObject(LinkedList.class, accounts);

      LOGGER.debug("[Response] " + response);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(response);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  /**
   * List transactions for the given address and currency.
   *
   * <p>Will only return data for addresses that belong to cosigner.
   *
   * @param params {@link CurrencyParameters} with the currency code and addresses filled in.
   * @return List of transactions that affect each account.
   */
  public static String listTransactions(String params) {
    try {
      CurrencyParameters currencyParams = convertParams(params);
      CurrencyPackageInterface currency = lookupCurrency(currencyParams);

      int returnNumber;
      int skipNumber;

      try {
        returnNumber = Integer.parseInt(currencyParams.getTransactionData().split(":")[0]);
      } catch (Exception e) {
        LOGGER.debug("No return number specified, using default of 100.");
        returnNumber = 100;
      }

      try {
        skipNumber = Integer.parseInt(currencyParams.getTransactionData().split(":")[1]);
      } catch (Exception e) {
        LOGGER.debug("No skip number specified, using default of 0.");
        skipNumber = 0;
      }

      LinkedList<TransactionDetails> txDetails = new LinkedList<>();
      final int finalReturnNumber = returnNumber;
      final int finalSkipNumber = skipNumber;
      currencyParams.getAccount().forEach(account -> txDetails.addAll(Arrays.asList(
          currency.getWallet().getTransactions(account, finalReturnNumber, finalSkipNumber))));

      String response = Json.stringifyObject(LinkedList.class, txDetails);

      LOGGER.debug("[Response] " + response);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(response);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  /**
   * Get transaction details for a specific transaction ID.
   */
  public static String getTransaction(String params) {
    try {
      CurrencyParameters currencyParams = convertParams(params);
      CurrencyPackageInterface currency = lookupCurrency(currencyParams);

      String response = Json.stringifyObject(TransactionDetails.class,
          currency.getWallet().getTransaction(currencyParams.getTransactionData()));
      LOGGER.debug("[Response] " + response);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(response);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  /**
   * Returns the combined balance of all addresses provided in the parameters.
   *
   * @param params {@link CurrencyParameters} with the currency code and addresses filled in.
   * @return Sum of all balances for the provided addresses.
   */
  public static String getBalance(String params) {
    try {
      CurrencyParameters currencyParams = convertParams(params);
      CurrencyPackageInterface currency = lookupCurrency(currencyParams);

      BigDecimal balance = BigDecimal.ZERO;
      if (currencyParams.getAccount() == null || currencyParams.getAccount().isEmpty()) {
        for (String account : currency.getWallet().getAddresses(currencyParams.getUserKey())) {
          balance = balance.add(new BigDecimal(currency.getWallet().getBalance(account)));
        }
      } else {
        for (String account : currencyParams.getAccount()) {
          balance = balance.add(new BigDecimal(currency.getWallet().getBalance(account)));
        }
      }

      String response = balance.toPlainString();

      LOGGER.debug("[Response] " + response);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(response);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  /**
   * Returns the combined pending balance of all addresses provided in the parameters.
   *
   * @param params {@link CurrencyParameters} with the currency code and addresses filled in.
   * @return Sum of all pending balances for the provided addresses.
   */
  public static String getPendingBalance(String params) {
    try {
      CurrencyParameters currencyParams = convertParams(params);
      CurrencyPackageInterface currency = lookupCurrency(currencyParams);

      BigDecimal balance = BigDecimal.ZERO;
      if (currencyParams.getAccount() == null || currencyParams.getAccount().isEmpty()) {
        for (String account : currency.getWallet().getAddresses(currencyParams.getUserKey())) {
          balance = balance.add(new BigDecimal(currency.getWallet().getPendingBalance(account)));
        }
      } else {
        for (String account : currencyParams.getAccount()) {
          balance = balance.add(new BigDecimal(currency.getWallet().getPendingBalance(account)));
        }
      }

      String response = balance.toPlainString();

      LOGGER.debug("[Response] " + response);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(response);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  private static void cleanUpSubscriptions(String id) {
    if (balanceSubscriptions.containsKey(id)) {
      balanceSubscriptions.get(id).forEach((s, subscription) -> subscription.unsubscribe());
      balanceSubscriptions.remove(id);
    }

    if (transactionSubscriptions.containsKey(id)) {
      transactionSubscriptions.get(id).forEach((s, subscription) -> subscription.unsubscribe());
      transactionSubscriptions.remove(id);
    }

    if (monitors.containsKey(id)) {
      monitors.get(id).forEach((s, monitor) -> monitor.destroyMonitor());
      monitors.remove(id);
    }
  }

  /**
   * Sets up a monitor for the given addresses.
   *
   * <p>A monitor provides periodic balance updates, along with all known transactions when
   * initialized, and any new transactions that come in while it's active. Transactions can be
   * distinguished from balance updates in that the transaction data portion of the response has
   * data, it contains the transaction hash.
   *
   * @param params         {@link CurrencyParameters} with the currency code and addresses filled
   *                       in. If using a REST callback, the callback needs to be filled in as
   *                       well.
   * @param responseSocket If this has been called using a web socket, pass the socket in here and
   *                       the data will be written to is as it's available.
   * @return An empty {@link CurrencyParameters} object is returned when the monitor is set up. The
   * actual data is sent through the socket or callback.
   */
  public static String monitorBalance(String params, Session responseSocket) {
    try {
      CurrencyParameters currencyParams = convertParams(params);

      CurrencyParameters returnParms = new CurrencyParameters();
      String response = Json.stringifyObject(CurrencyParameters.class, returnParms);

      // Web socket was passed to us
      if (responseSocket != null) {
        if (monitors.containsKey(responseSocket.toString())) {
          HashMap<String, Monitor> myMonitors = monitors.get(responseSocket.toString());
          LOGGER.debug("Found existing monitor set");
          if (myMonitors.containsKey(currencyParams.getCurrencySymbol())) {
            LOGGER.debug("Adding addresses to " + currencyParams.getCurrencySymbol());
            Monitor myMonitor = myMonitors.get(currencyParams.getCurrencySymbol());
            myMonitor.addAddresses(currencyParams.getAccount());
          }
        } else {
          LOGGER.debug("Creating new monitor set");
          cleanUpSubscriptions(responseSocket.toString());

          HashMap<String, Subscription> myBalances = new HashMap<>();
          HashMap<String, Subscription> myTransactions = new HashMap<>();
          HashMap<String, Monitor> myMonitors = new HashMap<>();
          CosignerApplication.getCurrencies().forEach((s, currencyPackageInterface) -> {
            LOGGER.debug("Creating monitor subscriptions for " + s);
            Monitor monitor = currencyPackageInterface.getMonitor().createNewMonitor();
            if (currencyParams.getCurrencySymbol().equals(s)) {
              LOGGER.debug("Adding addresses to monitor for " + s);
              monitor.addAddresses(currencyParams.getAccount());
            }

            Subscription wsBalanceSubscription =
                monitor.getObservableBalances().subscribe(balanceMap -> {
                  LOGGER.debug("Got balance tick for " + s);
                  LOGGER.debug(s + "Balance Map: " + Json.stringifyObject(Map.class, balanceMap));
                  balanceMap.forEach((address, balance) -> {
                    try {
                      CurrencyParameters responseParms = new CurrencyParameters();
                      responseParms.setCurrencySymbol(s);
                      responseParms.setAccount(new LinkedList<>());
                      responseParms.getAccount().add(address);
                      CurrencyParametersRecipient accountData = new CurrencyParametersRecipient();
                      accountData.setAmount(balance);
                      accountData.setRecipientAddress(address);
                      responseParms.setReceivingAccount(Collections.singletonList(accountData));
                      LOGGER.debug("Sending balance update...");
                      responseSocket.getRemote().sendString(
                          Json.stringifyObject(CurrencyParameters.class, responseParms));
                      responseSocket.getRemote().flush();
                    } catch (Exception e) {
                      LOGGER.debug(null, e);
                      cleanUpSubscriptions(responseSocket.toString());
                    }
                  });
                });

            Subscription wsTransactionSubscription =
                monitor.getObservableTransactions().subscribe(transactionSet -> {
                  LOGGER.debug("Got transaction tick for " + s);
                  transactionSet.forEach(transaction -> {
                    try {
                      CurrencyParameters responseParms = new CurrencyParameters();
                      responseParms.setCurrencySymbol(s);
                      responseParms.setAccount(new LinkedList<>());
                      responseParms.getAccount()
                          .addAll(Arrays.asList(transaction.getFromAddress()));
                      LinkedList<CurrencyParametersRecipient> receivers = new LinkedList<>();
                      Arrays.asList(transaction.getToAddress()).forEach(address -> {
                        CurrencyParametersRecipient sendData = new CurrencyParametersRecipient();
                        sendData.setAmount(transaction.getAmount().toPlainString());
                        sendData.setRecipientAddress(address);
                        receivers.add(sendData);
                      });
                      responseParms.setReceivingAccount(receivers);
                      responseParms.setTransactionData(transaction.getTxHash());
                      responseSocket.getRemote().sendString(
                          Json.stringifyObject(CurrencyParameters.class, responseParms));
                      responseSocket.getRemote().flush();
                    } catch (Exception e) {
                      LOGGER.debug(null, e);
                      cleanUpSubscriptions(responseSocket.toString());
                    }
                  });
                });

            myBalances.put(s, wsBalanceSubscription);
            myTransactions.put(s, wsTransactionSubscription);
            myMonitors.put(s, monitor);
          });
          balanceSubscriptions.put(responseSocket.toString(), myBalances);
          transactionSubscriptions.put(responseSocket.toString(), myTransactions);
          monitors.put(responseSocket.toString(), myMonitors);
        }
      } else if (currencyParams.getCallback() != null && !currencyParams.getCallback().isEmpty()) {
        // It's a REST callback
        if (monitors.containsKey(responseSocket.toString())) {
          HashMap<String, Monitor> myMonitors = monitors.get(responseSocket.toString());
          if (myMonitors.containsKey(currencyParams.getCurrencySymbol())) {
            Monitor myMonitor = myMonitors.get(currencyParams.getCurrencySymbol());
            myMonitor.addAddresses(currencyParams.getAccount());
          }
        } else {
          cleanUpSubscriptions(responseSocket.toString());

          HashMap<String, Subscription> myBalances = new HashMap<>();
          HashMap<String, Subscription> myTransactions = new HashMap<>();
          HashMap<String, Monitor> myMonitors = new HashMap<>();
          CosignerApplication.getCurrencies().forEach((s, currencyPackageInterface) -> {
            Monitor monitor = currencyPackageInterface.getMonitor().createNewMonitor();
            if (currencyParams.getCurrencySymbol().equals(s)) {
              monitor.addAddresses(currencyParams.getAccount());
            }

            Subscription rsBalanceSubscription =
                monitor.getObservableBalances().subscribe(balanceMap -> {
                  balanceMap.forEach((address, balance) -> {
                    try {
                      CurrencyParameters responseParms = new CurrencyParameters();
                      responseParms.setCurrencySymbol(s);
                      responseParms.setAccount(new LinkedList<>());
                      responseParms.getAccount().add(address);
                      CurrencyParametersRecipient accountData = new CurrencyParametersRecipient();
                      accountData.setAmount(balance);
                      accountData.setRecipientAddress(address);
                      responseParms.setReceivingAccount(Collections.singletonList(accountData));

                      HttpPost httpPost = new HttpPost(currencyParams.getCallback());
                      httpPost.addHeader("content-type", "application/json");
                      StringEntity entity;
                      entity = new StringEntity(
                          Json.stringifyObject(CurrencyParameters.class, responseParms));
                      httpPost.setEntity(entity);

                      HttpClients.createDefault().execute(httpPost).close();
                    } catch (Exception e) {
                      LOGGER.debug(null, e);
                      cleanUpSubscriptions(currencyParams.getCallback());
                    }
                  });
                });

            Subscription rsTransactionSubscription =
                monitor.getObservableTransactions().subscribe(transactionSet -> {
                  transactionSet.forEach(transaction -> {
                    try {
                      CurrencyParameters responseParms = new CurrencyParameters();
                      responseParms.setCurrencySymbol(s);
                      responseParms.setAccount(new LinkedList<>());
                      responseParms.getAccount()
                          .addAll(Arrays.asList(transaction.getFromAddress()));
                      LinkedList<CurrencyParametersRecipient> receivers = new LinkedList<>();
                      Arrays.asList(transaction.getToAddress()).forEach(address -> {
                        CurrencyParametersRecipient sendData = new CurrencyParametersRecipient();
                        sendData.setAmount(transaction.getAmount().toPlainString());
                        sendData.setRecipientAddress(address);
                        receivers.add(sendData);
                      });
                      responseParms.setReceivingAccount(receivers);
                      responseParms.setTransactionData(transaction.getTxHash());

                      HttpPost httpPost = new HttpPost(currencyParams.getCallback());
                      httpPost.addHeader("content-type", "application/json");
                      StringEntity entity;
                      entity = new StringEntity(
                          Json.stringifyObject(CurrencyParameters.class, responseParms));
                      httpPost.setEntity(entity);

                      HttpClients.createDefault().execute(httpPost).close();
                    } catch (Exception e) {
                      LOGGER.debug(null, e);
                      cleanUpSubscriptions(currencyParams.getCallback());
                    }
                  });
                });

            myBalances.put(s, rsBalanceSubscription);
            myTransactions.put(s, rsTransactionSubscription);
            myMonitors.put(s, monitor);
          });
          balanceSubscriptions.put(responseSocket.toString(), myBalances);
          transactionSubscriptions.put(responseSocket.toString(), myTransactions);
          monitors.put(responseSocket.toString(), myMonitors);
        }
      }

      LOGGER.debug("[Response] " + response);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(response);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  /**
   * Create and sign a transaction.
   *
   * <p>This only signs the transaction with the user's key, showing that the user has requested the
   * transaction. The server keys are not used until the approve stage.
   *
   * @param params {@link CurrencyParameters} with the currency code, user key, senders, recipients
   *               and amounts filled in.
   * @return The transaction string that was requested.
   */

  public static String prepareTransaction(String params) {
    try {
      CurrencyParameters currencyParams = convertParams(params);

      CurrencyPackageInterface currency = lookupCurrency(currencyParams);

      if (currencyParams.getTransactionData() == null || currencyParams.getTransactionData()
          .isEmpty()) {
        // Create the transaction
        List<String> addresses = new LinkedList<>();
        addresses.addAll(currencyParams.getAccount());
        LinkedList<Recipient> recipients = new LinkedList<>();
        currencyParams.getReceivingAccount().forEach(account -> {
          Recipient recipient = new Recipient();
          recipient.setAmount(new BigDecimal(account.getAmount()));
          recipient.setRecipientAddress(account.getRecipientAddress());
          recipients.add(recipient);
        });
        currencyParams.setTransactionData(currency.getWallet()
            .createTransaction(addresses, recipients, currencyParams.getOptions()));
      }

      // Authorize it with the user account
      String initalTx = currencyParams.getTransactionData();
      LOGGER.debug("Wallet.CreateTransaction Result: " + initalTx);

      if (currencyParams.getUserKey() != null && !currencyParams.getUserKey().isEmpty()) {
        currencyParams.setTransactionData(currency.getWallet()
            .signTransaction(initalTx, currencyParams.getAccount().get(0),
                currencyParams.getUserKey(), currencyParams.getOptions()));
        LOGGER.debug("Sign with userKey: " + currencyParams.getTransactionData());
      }

      // If the userKey/address combo don't work then we stop here.
      if (currencyParams.getTransactionData().equalsIgnoreCase(initalTx)) {
        LOGGER.debug("No userKey signature, returning unsigned TX: " + initalTx);
        CosignerResponse cosignerResponse = new CosignerResponse();
        cosignerResponse.setResult(initalTx);
        return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
      }

      // Try to validate it, don't sign if it fails.
      for (Validator validator : CosignerApplication.getValidators()) {
        if (!validator.validateTransaction(currency, currencyParams.getTransactionData())) {
          throw new Exception("Validation failed, transaction not ok.");
        }
      }

      // Send it if it's a sign-each and there's more than one signature
      // required (we're at 1/X)
      if (currency.getConfiguration().getMinSignatures() > 1 && currency.getConfiguration()
          .getSigningType().equals(SigningType.SENDEACH)) {
        submitTransaction(Json.stringifyObject(CurrencyParameters.class, currencyParams));
      }

      String response = currencyParams.getTransactionData();
      LOGGER.debug("[Response] " + response);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(response);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  /**
   * Get list of addresses that could sign this transaction.
   */
  public static String getSignersForTransaction(String params) {
    try {
      CurrencyParameters currencyParams = convertParams(params);
      CurrencyPackageInterface currency = lookupCurrency(currencyParams);

      Iterable<String> signers =
          currency.getWallet().getSignersForTransaction(currencyParams.getTransactionData());

      String response = Json.stringifyObject(Iterable.class, signers);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(response);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  /**
   * Get the data needed to create an offline signature for the transaction.
   */
  public static String getSignatureString(String params) {
    try {
      CurrencyParameters currencyParams = convertParams(params);
      CurrencyPackageInterface currency = lookupCurrency(currencyParams);

      String address = "";
      if (currencyParams.getAccount() != null && currencyParams.getAccount().size() > 0) {
        address = currencyParams.getAccount().get(0);
      }

      Iterable<Iterable<String>> sigData =
          currency.getWallet().getSigString(currencyParams.getTransactionData(), address);

      String response = Json.stringifyObject(Iterable.class, sigData);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(response);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  /**
   * Apply an offline signature to a transaction.
   */
  @SuppressWarnings("unchecked")
  public static String applySignature(String params) {
    try {
      CurrencyParameters currencyParams = convertParams(params);
      CurrencyPackageInterface currency = lookupCurrency(currencyParams);

      String address = "";
      if (currencyParams.getAccount() != null && currencyParams.getAccount().size() > 0) {
        address = currencyParams.getAccount().get(0);
      }

      Iterator<String> txList = ((Iterable<String>) Json
          .objectifyString(Iterable.class, currencyParams.getTransactionData())).iterator();
      String tx = txList.next();
      Iterable<Iterable<String>> sigData =
          (Iterable<Iterable<String>>) Json.objectifyString(Iterable.class, txList.next());

      String response = currency.getWallet().applySignature(tx, address, sigData);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(response);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  /**
   * Approve a transaction that's been signed off on by the user.
   *
   * <p>This stage signs the transaction with the server keys after running it through any sanity
   * checks and validation required.
   *
   * @param params        {@link CurrencyParameters} with the currency code, user key, senders,
   *                      recipients and amounts filled in. The transaction data should be filled in
   *                      with the response from prepareTransaction.
   * @param sendToRemotes Indicates whether cosigner should attempt to request signature from any
   *                      other cosigner servers in the cluster.
   * @return Signed transaction string
   */
  public static String approveTransaction(String params, boolean sendToRemotes) {
    try {
      CurrencyParameters currencyParams = convertParams(params);
      CurrencyPackageInterface currency = lookupCurrency(currencyParams);

      for (Server server : ClusterInfo.getInstance().getServers()) {
        if (server.isOriginator()) { // It's us, try to sign it locally.
          // But first check that it's valid.
          for (Validator validator : CosignerApplication.getValidators()) {
            if (!validator.validateTransaction(currency, currencyParams.getTransactionData())) {
              throw new Exception("Transaction could not be validated.");
            }
          }

          // Apply user-key signature first if it exists.
          if (currencyParams.getUserKey() != null && !currencyParams.getUserKey().isEmpty()) {
            String sigAttempt = currency.getWallet()
                .signTransaction(currencyParams.getTransactionData(),
                    currencyParams.getAccount().get(0), currencyParams.getUserKey(),
                    currencyParams.getOptions());
            currencyParams.setTransactionData(sigAttempt);
          }

          String sigAttempt = currency.getWallet()
              .signTransaction(currencyParams.getTransactionData(),
                  currencyParams.getAccount().get(0), null, currencyParams.getOptions());
          currencyParams.setTransactionData(sigAttempt);
        } else if (sendToRemotes) {
          try {
            CurrencyCommand command = new CurrencyCommand();
            CurrencyParameters copyParams = convertParams(params);
            // Don't want every server trying to sign with the user-key.
            copyParams.setUserKey("");
            command.setCurrencyParams(copyParams);
            command.setCommandType(CurrencyCommandType.SIGN);
            CosignerResponse cosignerResponse = (CosignerResponse) Json
                .objectifyString(CosignerResponse.class,
                    Coordinator.broadcastCommand(command, server));

            if (cosignerResponse.getError() != null && !cosignerResponse.getError().isEmpty()) {
              throw new Exception(cosignerResponse.getError());
            }

            String originalTx = currencyParams.getTransactionData();
            currencyParams.setTransactionData(cosignerResponse.getResult());

            // If it's send-each and the remote actually signed it, send it.
            if (!originalTx.equalsIgnoreCase(currencyParams.getTransactionData()) && currency
                .getConfiguration().getSigningType().equals(SigningType.SENDEACH)) {
              submitTransaction(Json.stringifyObject(CurrencyParameters.class, currencyParams));
            }

          } catch (Exception e) {
            // Likely caused by an offline server or bad response.
            LOGGER.warn(null, e);
          }
        }
      }
      String response = currencyParams.getTransactionData();

      LOGGER.debug("[Response] " + response);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(response);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

  /**
   * Submits a transaction for processing on the network.
   *
   * @param params {@link CurrencyParameters} with the currency and transaction data filled in. The
   *               transaction data required is the result from the approveTransaction stage.
   * @return The transaction hash/ID.
   */
  public static String submitTransaction(String params) {
    try {
      CurrencyParameters currencyParams = convertParams(params);
      CurrencyPackageInterface currency = lookupCurrency(currencyParams);

      String response = currency.getWallet().sendTransaction(currencyParams.getTransactionData());

      LOGGER.debug("[Response] " + response);
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setResult(response);
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    } catch (Exception e) {
      CosignerResponse cosignerResponse = new CosignerResponse();
      cosignerResponse.setError(e.toString());
      return Json.stringifyObject(CosignerResponse.class, cosignerResponse);
    }
  }

}
