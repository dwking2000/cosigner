package io.emax.heimdal.core.currency;

import java.util.Arrays;
import java.util.LinkedList;

import org.junit.Test;

import io.emax.heimdal.api.currency.CurrencyConfiguration;
import io.emax.heimdal.api.currency.Monitor;
import io.emax.heimdal.api.currency.Wallet;
import io.emax.heimdal.bitcoin.stubrpc.BitcoinTestRpc;
import io.emax.heimdal.core.Application;
import io.emax.heimdal.ethereum.stubrpc.EthereumTestRpc;
import junit.framework.TestCase;

public class CommonTest extends TestCase {
  // General
  private static String userKey;

  // Bitcoin
  private static Wallet bitcoinWallet = new io.emax.heimdal.bitcoin.Wallet(new BitcoinTestRpc());
  private static Monitor bitcoinMonitor =
      new io.emax.heimdal.bitcoin.Monitor((io.emax.heimdal.bitcoin.Wallet) bitcoinWallet);
  private static CurrencyConfiguration bitcoinConfig =
      new io.emax.heimdal.bitcoin.CurrencyConfiguration();

  // Ethereum
  private static Wallet ethereumWallet = new io.emax.heimdal.ethereum.Wallet(new EthereumTestRpc());
  private static Monitor ethereumMonitor =
      new io.emax.heimdal.ethereum.Monitor((io.emax.heimdal.ethereum.Wallet) ethereumWallet);
  private static CurrencyConfiguration ethereumConfig =
      new io.emax.heimdal.ethereum.CurrencyConfiguration();

  @Override
  public void setUp() {
    // General setup
    userKey = "deadbeef";

    // Register currency packages
    CurrencyPackage bitcoinPackage = new CurrencyPackage();
    bitcoinPackage.setConfiguration(bitcoinConfig);
    bitcoinPackage.setMonitor(bitcoinMonitor);
    bitcoinPackage.setWallet(bitcoinWallet);

    CurrencyPackage ethereumPackage = new CurrencyPackage();
    ethereumPackage.setConfiguration(ethereumConfig);
    ethereumPackage.setMonitor(ethereumMonitor);
    ethereumPackage.setWallet(ethereumWallet);

    Application.getCurrencies().put(bitcoinPackage.getConfiguration().getCurrencySymbol(),
        bitcoinPackage);
    Application.getCurrencies().put(ethereumPackage.getConfiguration().getCurrencySymbol(),
        ethereumPackage);
  }

  @Test
  public void testGetCurrencies() {
    System.out.println("");
    System.out.println("Listing currencies");

    try {
      String currenciesString = Common.getCurrencies();
      @SuppressWarnings("unchecked")
      LinkedList<String> currencies =
          (LinkedList<String>) Common.objectifyString(LinkedList.class, currenciesString);

      currencies.forEach(currency -> {
        System.out.println("Found currency: " + currency);
      });
    } catch (Exception e) {
      e.printStackTrace();
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
      String currenciesString = Common.getCurrencies();
      currencies = (LinkedList<String>) Common.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAccount(parmsString);
        System.out.println(address);
      });
    } catch (Exception e) {
      e.printStackTrace();
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
      String currenciesString = Common.getCurrencies();
      currencies = (LinkedList<String>) Common.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        String addressString = Common.listAllAccounts(parmsString);
        
        LinkedList<String> addresses = (LinkedList<String>) Common.objectifyString(LinkedList.class, addressString);
        addresses.forEach(address -> {
          System.out.println(address);
        });
      });
    } catch (Exception e) {
      e.printStackTrace();
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
      String currenciesString = Common.getCurrencies();
      currencies = (LinkedList<String>) Common.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAccount(parmsString);
        
        parms.setAccount(Arrays.asList(address));
        parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        String balance = Common.getBalance(parmsString);
        System.out.println(balance);
      });
    } catch (Exception e) {
      e.printStackTrace();
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
      String currenciesString = Common.getCurrencies();
      currencies = (LinkedList<String>) Common.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAccount(parmsString);
        
        parms.setAccount(Arrays.asList(address));
        parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        String balance = Common.monitorBalance(parmsString, null);
        System.out.println(balance);
      });
    } catch (Exception e) {
      e.printStackTrace();
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
      String currenciesString = Common.getCurrencies();
      currencies = (LinkedList<String>) Common.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAccount(parmsString);
        
        parms.setAccount(Arrays.asList(address));
        address = Common.getNewAccount(parmsString);
        parms.setReceivingAccount(address);
        parms.setAmount("5.0");
        parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        
        String tx = Common.prepareTransaction(parmsString);
        System.out.println(tx);
      });
    } catch (Exception e) {
      e.printStackTrace();
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
      String currenciesString = Common.getCurrencies();
      currencies = (LinkedList<String>) Common.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAccount(parmsString);
        
        parms.setAccount(Arrays.asList(address));
        address = Common.getNewAccount(parmsString);
        parms.setReceivingAccount(address);
        parms.setAmount("5.0");
        parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        
        String tx = Common.prepareTransaction(parmsString);
        parms.setTransactionData(tx);
        parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        
        tx = Common.approveTransaction(parmsString);
        System.out.println(tx);        
      });
    } catch (Exception e) {
      e.printStackTrace();
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
      String currenciesString = Common.getCurrencies();
      currencies = (LinkedList<String>) Common.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Problem listing currencies.");
    }

    try {
      currencies.forEach(currency -> {
        System.out.println("For " + currency);
        CurrencyParameters parms = new CurrencyParameters();
        parms.setCurrencySymbol(currency);
        parms.setUserKey(userKey);

        String parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        String address = Common.getNewAccount(parmsString);
        
        parms.setAccount(Arrays.asList(address));
        address = Common.getNewAccount(parmsString);
        parms.setReceivingAccount(address);
        parms.setAmount("5.0");
        parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        
        String tx = Common.prepareTransaction(parmsString);
        parms.setTransactionData(tx);
        parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        
        tx = Common.approveTransaction(parmsString);
        parms.setTransactionData(tx);
        parmsString = Common.stringifyObject(CurrencyParameters.class, parms);
        
        tx = Common.submitTransaction(parmsString);
        System.out.println(tx);        
      });
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception when preparing up a transaction");
    }
  }

}
