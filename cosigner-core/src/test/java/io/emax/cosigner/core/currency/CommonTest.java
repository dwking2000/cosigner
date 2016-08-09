package io.emax.cosigner.core.currency;

import io.emax.cosigner.api.core.CosignerResponse;
import io.emax.cosigner.api.core.CurrencyPackageInterface;
import io.emax.cosigner.api.core.CurrencyParameters;
import io.emax.cosigner.api.core.CurrencyParametersRecipient;
import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.Monitor;
import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.bitcoin.BitcoinConfiguration;
import io.emax.cosigner.bitcoin.BitcoinCurrencyPackage;
import io.emax.cosigner.bitcoin.BitcoinMonitor;
import io.emax.cosigner.bitcoin.BitcoinResource;
import io.emax.cosigner.bitcoin.BitcoinWallet;
import io.emax.cosigner.bitcoin.stubrpc.BitcoinTestRpc;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.core.CosignerApplication;
import io.emax.cosigner.ethereum.EthereumConfiguration;
import io.emax.cosigner.ethereum.EthereumCurrencyPackage;
import io.emax.cosigner.ethereum.EthereumMonitor;
import io.emax.cosigner.ethereum.EthereumResource;
import io.emax.cosigner.ethereum.EthereumWallet;
import io.emax.cosigner.ethereum.stubrpc.EthereumTestRpc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;

public class CommonTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(CommonTest.class);

  // General
  private String userKey;

  @Before
  public void setUp() {
    // General setup
    userKey = "deadbeef";

    // Bitcoin
    BitcoinResource.getResource().setBitcoindRpc(new BitcoinTestRpc());
    CurrencyConfiguration bitcoinConfig = new BitcoinConfiguration();
    Wallet bitcoinWallet = new BitcoinWallet((BitcoinConfiguration) bitcoinConfig);
    Monitor bitcoinMonitor = new BitcoinMonitor((BitcoinWallet) bitcoinWallet);

    // Ethereum
    EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
    CurrencyConfiguration ethereumConfig = new EthereumConfiguration();
    Wallet ethereumWallet = new EthereumWallet((EthereumConfiguration) ethereumConfig);
    Monitor ethereumMonitor = new EthereumMonitor((EthereumWallet) ethereumWallet);

    // Register currency packages
    CurrencyPackageInterface bitcoinPackage = new BitcoinCurrencyPackage();
    bitcoinPackage.setConfiguration(bitcoinConfig);
    bitcoinPackage.setMonitor(bitcoinMonitor);
    bitcoinPackage.setWallet(bitcoinWallet);

    CurrencyPackageInterface ethereumPackage = new EthereumCurrencyPackage();
    ethereumPackage.setConfiguration(ethereumConfig);
    ethereumPackage.setMonitor(ethereumMonitor);
    ethereumPackage.setWallet(ethereumWallet);

    CosignerApplication.getCurrencies()
        .put(bitcoinPackage.getConfiguration().getCurrencySymbol(), bitcoinPackage);
    CosignerApplication.getCurrencies()
        .put(ethereumPackage.getConfiguration().getCurrencySymbol(), ethereumPackage);

  }

  @Test
  public void testGetCurrencies() {
    System.out.println("");
    System.out.println("Listing currencies");

    try {
      String currenciesString = Common.listCurrencies();
      CosignerResponse cosignerResponse =
          (CosignerResponse) Json.objectifyString(CosignerResponse.class, currenciesString);
      System.out.println("Got response: " + currenciesString);

      currenciesString = cosignerResponse.getResult();
      @SuppressWarnings("unchecked") LinkedList<String> currencies =
          (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);

      currencies.forEach(currency -> System.out.println("Found currency: " + currency));
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Problem listing currencies.");
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
      CosignerResponse cosignerResponse =
          (CosignerResponse) Json.objectifyString(CosignerResponse.class, currenciesString);
      currenciesString = cosignerResponse.getResult();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAddress(parmsString);
        CosignerResponse cosignerResponse =
            (CosignerResponse) Json.objectifyString(CosignerResponse.class, address);
        System.out.println(cosignerResponse.getResult());
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Exception when creating addresses");
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
      CosignerResponse cosignerResponse =
          (CosignerResponse) Json.objectifyString(CosignerResponse.class, currenciesString);
      currenciesString = cosignerResponse.getResult();

      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String addressString = Common.listAllAddresses(parmsString);
        CosignerResponse cosignerResponse =
            (CosignerResponse) Json.objectifyString(CosignerResponse.class, addressString);
        addressString = cosignerResponse.getResult();

        LinkedList<String> addresses =
            (LinkedList<String>) Json.objectifyString(LinkedList.class, addressString);
        addresses.forEach(System.out::println);
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Exception when listing addresses");
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
      CosignerResponse cosignerResponse =
          (CosignerResponse) Json.objectifyString(CosignerResponse.class, currenciesString);
      currenciesString = cosignerResponse.getResult();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAddress(parmsString);
        CosignerResponse cosignerResponse =
            (CosignerResponse) Json.objectifyString(CosignerResponse.class, address);
        address = cosignerResponse.getResult();

        parms.setAccount(Collections.singletonList(address));
        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String balance = Common.getBalance(parmsString);
        cosignerResponse = (CosignerResponse) Json.objectifyString(CosignerResponse.class, balance);
        balance = cosignerResponse.getResult();
        System.out.println(balance);
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Exception when checking balances");
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
      CosignerResponse cosignerResponse =
          (CosignerResponse) Json.objectifyString(CosignerResponse.class, currenciesString);
      currenciesString = cosignerResponse.getResult();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAddress(parmsString);
        CosignerResponse cosignerResponse =
            (CosignerResponse) Json.objectifyString(CosignerResponse.class, address);
        address = cosignerResponse.getResult();
        parms.setAccount(Collections.singletonList(address));

        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String balance = Common.monitorBalance(parmsString, null);
        cosignerResponse = (CosignerResponse) Json.objectifyString(CosignerResponse.class, balance);
        System.out.println(balance);

        // Request a second address
        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        address = Common.getNewAddress(parmsString);
        cosignerResponse = (CosignerResponse) Json.objectifyString(CosignerResponse.class, address);
        address = cosignerResponse.getResult();
        parms.setAccount(Collections.singletonList(address));

        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        balance = Common.monitorBalance(parmsString, null);
        cosignerResponse = (CosignerResponse) Json.objectifyString(CosignerResponse.class, balance);
        System.out.println(balance);
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Exception when setting up a monitor");
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
      CosignerResponse cosignerResponse =
          (CosignerResponse) Json.objectifyString(CosignerResponse.class, currenciesString);
      currenciesString = cosignerResponse.getResult();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Problem listing currencies.");
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
          CosignerResponse cosignerResponse =
              (CosignerResponse) Json.objectifyString(CosignerResponse.class, address);
          address = cosignerResponse.getResult();

          parms.setAccount(Collections.singletonList(address));
          address = Common.getNewAddress(parmsString);
          cosignerResponse =
              (CosignerResponse) Json.objectifyString(CosignerResponse.class, address);
          address = cosignerResponse.getResult();
          CurrencyParametersRecipient accountData = new CurrencyParametersRecipient();
          accountData.setAmount("5.0");
          accountData.setRecipientAddress(address);
          parms.setReceivingAccount(Collections.singletonList(accountData));
          parmsString = Json.stringifyObject(CurrencyParameters.class, parms);

          String tx = Common.prepareTransaction(parmsString);
          cosignerResponse = (CosignerResponse) Json.objectifyString(CosignerResponse.class, tx);
          tx = cosignerResponse.getResult();
          System.out.println(tx);
        } catch (Exception e) {
          LOGGER.debug(null, e);
          Assert.fail("Exception when preparing a transaction.");
        }
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Exception when preparing up a transaction");
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSignAndApply() {
    System.out.println("");
    System.out.println("Sign and apply a transaction for all currencies.");

    LinkedList<String> currencies = new LinkedList<>();
    try {
      String currenciesString = Common.listCurrencies();
      CosignerResponse cosignerResponse =
          (CosignerResponse) Json.objectifyString(CosignerResponse.class, currenciesString);
      currenciesString = cosignerResponse.getResult();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        try {
          System.out.println("For " + currency);
          CurrencyParameters parms = new CurrencyParameters();
          parms.setCurrencySymbol(currency);
          parms.setUserKey(userKey);

          CurrencyPackageInterface currencyPackage = Common.lookupCurrency(parms);
          String parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
          String privateKey = currencyPackage.getWallet().generatePrivateKey();
          String address = currencyPackage.getWallet().createAddressFromKey(privateKey, true);

          parms.setAccount(Collections.singletonList(address));
          address = Common.getNewAddress(parmsString);
          CosignerResponse cosignerResponse =
              (CosignerResponse) Json.objectifyString(CosignerResponse.class, address);
          address = cosignerResponse.getResult();
          CurrencyParametersRecipient accountData = new CurrencyParametersRecipient();
          accountData.setAmount("5.0");
          accountData.setRecipientAddress(address);
          parms.setReceivingAccount(Collections.singletonList(accountData));
          parmsString = Json.stringifyObject(CurrencyParameters.class, parms);

          String tx = Common.prepareTransaction(parmsString);
          cosignerResponse = (CosignerResponse) Json.objectifyString(CosignerResponse.class, tx);
          tx = cosignerResponse.getResult();
          System.out.println(tx);

          parms.setTransactionData(tx);
          parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
          String sigData = Common.getSignatureString(parmsString);
          cosignerResponse =
              (CosignerResponse) Json.objectifyString(CosignerResponse.class, sigData);
          sigData = cosignerResponse.getResult();
          System.out.println(sigData);

          Iterable<Iterable<String>> signatureData =
              (Iterable<Iterable<String>>) Json.objectifyString(Iterable.class, sigData);
          signatureData = currencyPackage.getWallet().signWithPrivateKey(signatureData, privateKey);
          sigData = Json.stringifyObject(Iterable.class, signatureData);
          System.out.println(sigData);

          LinkedList<String> sigApplication = new LinkedList<>();
          sigApplication.add(tx);
          sigApplication.add(sigData);
          parms.setTransactionData(Json.stringifyObject(Iterable.class, sigApplication));
          parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
          tx = Common.applySignature(parmsString);
          cosignerResponse = (CosignerResponse) Json.objectifyString(CosignerResponse.class, tx);
          tx = cosignerResponse.getResult();
          System.out.println(tx);

        } catch (Exception e) {
          LOGGER.debug(null, e);
          Assert.fail("Exception when preparing a transaction.");
        }
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Exception when preparing up a transaction");
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
      CosignerResponse cosignerResponse =
          (CosignerResponse) Json.objectifyString(CosignerResponse.class, currenciesString);
      currenciesString = cosignerResponse.getResult();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAddress(parmsString);
        CosignerResponse cosignerResponse =
            (CosignerResponse) Json.objectifyString(CosignerResponse.class, address);
        address = cosignerResponse.getResult();

        parms.setAccount(Collections.singletonList(address));
        address = Common.getNewAddress(parmsString);
        cosignerResponse = (CosignerResponse) Json.objectifyString(CosignerResponse.class, address);
        address = cosignerResponse.getResult();
        CurrencyParametersRecipient accountData = new CurrencyParametersRecipient();
        accountData.setAmount("5.0");
        accountData.setRecipientAddress(address);
        parms.setReceivingAccount(Collections.singletonList(accountData));
        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);

        String tx = Common.prepareTransaction(parmsString);
        cosignerResponse = (CosignerResponse) Json.objectifyString(CosignerResponse.class, tx);
        tx = cosignerResponse.getResult();
        parms.setTransactionData(tx);
        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);

        tx = Common.approveTransaction(parmsString, true);
        cosignerResponse = (CosignerResponse) Json.objectifyString(CosignerResponse.class, tx);
        tx = cosignerResponse.getResult();
        System.out.println(tx);
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Exception when preparing up a transaction");
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
      CosignerResponse cosignerResponse =
          (CosignerResponse) Json.objectifyString(CosignerResponse.class, currenciesString);
      currenciesString = cosignerResponse.getResult();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Json.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAddress(parmsString);
        CosignerResponse cosignerResponse =
            (CosignerResponse) Json.objectifyString(CosignerResponse.class, address);
        address = cosignerResponse.getResult();

        parms.setAccount(Collections.singletonList(address));
        address = Common.getNewAddress(parmsString);
        cosignerResponse = (CosignerResponse) Json.objectifyString(CosignerResponse.class, address);
        address = cosignerResponse.getResult();
        CurrencyParametersRecipient accountData = new CurrencyParametersRecipient();
        accountData.setAmount("5.0");
        accountData.setRecipientAddress(address);
        parms.setReceivingAccount(Collections.singletonList(accountData));
        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);

        String tx = Common.prepareTransaction(parmsString);
        cosignerResponse = (CosignerResponse) Json.objectifyString(CosignerResponse.class, tx);
        tx = cosignerResponse.getResult();
        parms.setTransactionData(tx);
        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);

        tx = Common.approveTransaction(parmsString, true);
        cosignerResponse = (CosignerResponse) Json.objectifyString(CosignerResponse.class, tx);
        tx = cosignerResponse.getResult();
        parms.setTransactionData(tx);
        parmsString = Json.stringifyObject(CurrencyParameters.class, parms);

        tx = Common.submitTransaction(parmsString);
        cosignerResponse = (CosignerResponse) Json.objectifyString(CosignerResponse.class, tx);
        tx = cosignerResponse.getResult();
        System.out.println(tx);
      });
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Exception when preparing up a transaction");
    }
  }

}
