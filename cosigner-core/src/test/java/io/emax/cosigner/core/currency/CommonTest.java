package io.emax.cosigner.core.currency;

import java.util.Arrays;
import java.util.LinkedList;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.emax.cosigner.api.core.CurrencyPackage;
import io.emax.cosigner.api.core.CurrencyParameters;
import io.emax.cosigner.api.core.CurrencyParametersRecipient;
import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.Monitor;
import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.bitcoin.BitcoinResource;
import io.emax.cosigner.bitcoin.stubrpc.BitcoinTestRpc;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.core.CosignerApplication;
import io.emax.cosigner.ethereum.EthereumResource;
import io.emax.cosigner.ethereum.stubrpc.EthereumTestRpc;
import junit.framework.TestCase;

public class CommonTest extends TestCase {
  private static final Logger LOGGER = LoggerFactory.getLogger(CommonTest.class);

  // General
  private String userKey;

  // Bitcoin
  private Wallet bitcoinWallet;
  private Monitor bitcoinMonitor;
  private CurrencyConfiguration bitcoinConfig;

  // Ethereum
  private Wallet ethereumWallet;
  private Monitor ethereumMonitor;
  private CurrencyConfiguration ethereumConfig;

  @Override
  public void setUp() {
    // General setup
    userKey = "deadbeef";

    // Bitcoin
    BitcoinResource.getResource().setBitcoindRpc(new BitcoinTestRpc());
    bitcoinWallet = new io.emax.cosigner.bitcoin.BitcoinWallet();
    bitcoinMonitor = new io.emax.cosigner.bitcoin.BitcoinMonitor();
    bitcoinConfig = new io.emax.cosigner.bitcoin.BitcoinConfiguration();

    // Ethereum
    EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
    ethereumWallet = new io.emax.cosigner.ethereum.EthereumWallet();
    ethereumMonitor = new io.emax.cosigner.ethereum.EthereumMonitor();
    ethereumConfig = new io.emax.cosigner.ethereum.EthereumConfiguration();

    // Register currency packages
    CurrencyPackage bitcoinPackage = new CurrencyPackage();
    bitcoinPackage.setConfiguration(bitcoinConfig);
    bitcoinPackage.setMonitor(bitcoinMonitor);
    bitcoinPackage.setWallet(bitcoinWallet);

    CurrencyPackage ethereumPackage = new CurrencyPackage();
    ethereumPackage.setConfiguration(ethereumConfig);
    ethereumPackage.setMonitor(ethereumMonitor);
    ethereumPackage.setWallet(ethereumWallet);

    CosignerApplication.getCurrencies().put(bitcoinPackage.getConfiguration().getCurrencySymbol(),
        bitcoinPackage);
    CosignerApplication.getCurrencies().put(ethereumPackage.getConfiguration().getCurrencySymbol(),
        ethereumPackage);

  }

  @Test
  public void testGetCurrencies() {
    System.out.println("");
    System.out.println("Listing currencies");

    try {
      String currenciesString = Common.listCurrencies();
      @SuppressWarnings("unchecked")
      LinkedList<String> currencies =
          (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);

      currencies.forEach(currency -> {
        System.out.println("Found currency: " + currency);
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Problem listing currencies.");
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetNewAccount() {
    System.out.println("");
    System.out.println("Generating a new account for all currencies.");

    LinkedList<String> currencies = new LinkedList<>();
    try {
      String currenciesString = Common.listCurrencies();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAddress(parmsString);
        System.out.println(address);
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Exception when creating addresses");
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testListAllAccounts() {
    System.out.println("");
    System.out.println("Listing accounts for all currencies.");

    LinkedList<String> currencies = new LinkedList<>();
    try {
      String currenciesString = Common.listCurrencies();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String addressString = Common.listAllAddresses(parmsString);

        LinkedList<String> addresses =
            (LinkedList<String>) Json.objectifyString(LinkedList.class, addressString);
        addresses.forEach(address -> {
          System.out.println(address);
        });
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Exception when listing addresses");
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetBalance() {
    System.out.println("");
    System.out.println("Getting an account balance for all currencies.");

    LinkedList<String> currencies = new LinkedList<>();
    try {
      String currenciesString = Common.listCurrencies();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAddress(parmsString);

        parms.setAccount(Arrays.asList(address));
        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String balance = Common.getBalance(parmsString);
        System.out.println(balance);
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Exception when checking balances");
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testMonitorBalance() {
    System.out.println("");
    System.out.println("Registering a monitor for all currencies.");

    LinkedList<String> currencies = new LinkedList<>();
    try {
      String currenciesString = Common.listCurrencies();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAddress(parmsString);

        parms.setAccount(Arrays.asList(address));
        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String balance = Common.monitorBalance(parmsString, null);
        System.out.println(balance);
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Exception when setting up a monitor");
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPrepareTransaction() {
    System.out.println("");
    System.out.println("Preparing a transaction for all currencies.");

    LinkedList<String> currencies = new LinkedList<>();
    try {
      String currenciesString = Common.listCurrencies();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        try {
          System.out.println("For " + currency);
          CurrencyParameters parms = new CurrencyParameters();
          parms.setCurrencySymbol(currency);
          parms.setUserKey(userKey);

          String parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
          String address = Common.getNewAddress(parmsString);

          parms.setAccount(Arrays.asList(address));
          address = Common.getNewAddress(parmsString);
          CurrencyParametersRecipient accountData = new CurrencyParametersRecipient();
          accountData.setAmount("5.0");
          accountData.setRecipientAddress(address);
          parms.setReceivingAccount(Arrays.asList(accountData));
          parmsString = Json.stringifyObject(CurrencyParameters.class, parms);

          String tx = Common.prepareTransaction(parmsString);
          System.out.println(tx);
        } catch (Exception e) {
          LOGGER.debug(null, e);
          fail("Exception when preparing a transaction.");
        }
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Exception when preparing up a transaction");
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testApproveTransaction() {
    System.out.println("");
    System.out.println("Approving a transaction for all currencies.");

    LinkedList<String> currencies = new LinkedList<>();
    try {
      String currenciesString = Common.listCurrencies();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAddress(parmsString);

        parms.setAccount(Arrays.asList(address));
        address = Common.getNewAddress(parmsString);
        CurrencyParametersRecipient accountData = new CurrencyParametersRecipient();
        accountData.setAmount("5.0");
        accountData.setRecipientAddress(address);
        parms.setReceivingAccount(Arrays.asList(accountData));
        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);

        String tx = Common.prepareTransaction(parmsString);
        parms.setTransactionData(tx);
        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);

        tx = Common.approveTransaction(parmsString, true);
        System.out.println(tx);
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Exception when preparing up a transaction");
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSubmitTransaction() {
    System.out.println("");
    System.out.println("Approving a transaction for all currencies.");

    LinkedList<String> currencies = new LinkedList<>();
    try {
      String currenciesString = Common.listCurrencies();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAddress(parmsString);

        parms.setAccount(Arrays.asList(address));
        address = Common.getNewAddress(parmsString);
        CurrencyParametersRecipient accountData = new CurrencyParametersRecipient();
        accountData.setAmount("5.0");
        accountData.setRecipientAddress(address);
        parms.setReceivingAccount(Arrays.asList(accountData));
        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);

        String tx = Common.prepareTransaction(parmsString);
        parms.setTransactionData(tx);
        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);

        tx = Common.approveTransaction(parmsString, true);
        parms.setTransactionData(tx);
        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);

        tx = Common.submitTransaction(parmsString);
        System.out.println(tx);
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      fail("Exception when preparing up a transaction");
    }
  }

}
