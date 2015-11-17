package io.emax.cosigner.bitcoin;

import io.emax.cosigner.api.currency.Wallet.Recipient;
import io.emax.cosigner.bitcoin.bitcoindrpc.RawTransaction;
import io.emax.cosigner.bitcoin.stubrpc.BitcoinTestRpc;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.util.Arrays;

public class WalletTest extends TestCase {
  private static final Logger LOGGER = LoggerFactory.getLogger(WalletTest.class);
  private BitcoinWallet wallet;
  private String userKey;

  @Override
  public void setUp() {
    BitcoinResource.getResource().setBitcoindRpc(new BitcoinTestRpc());
    wallet = new BitcoinWallet();
    userKey = "deadbeef";
  }

  @Test
  public void testWallet() {
    System.out.println("");
    System.out.println("Simple wallet test. Should be no exceptions");
    try {
      String firstAddress = wallet.createAddress(userKey);
      System.out.println("First Address: " + firstAddress);
      String secondAddress = wallet.createAddress(userKey + userKey);
      System.out.println("Second Address: " + secondAddress);

      Recipient recipient = new Recipient();
      recipient.setAmount(BigDecimal.valueOf(20));
      recipient.setRecipientAddress(secondAddress);
      String txString =
          wallet.createTransaction(Arrays.asList(firstAddress), Arrays.asList(recipient));
      System.out.println("20 BTC from " + firstAddress + " to " + secondAddress + ": " + txString);
      System.out.println(RawTransaction.parse(txString));
      txString = wallet.signTransaction(txString, firstAddress, userKey);
      System.out.println("Signed TX: " + txString);
      System.out.println(RawTransaction.parse(txString));
      txString = wallet.sendTransaction(txString);
      System.out.println("TX ID: " + txString);
    } catch (Exception e) {
      LOGGER.error(null, e);
      fail("Simple wallet test failed!");
    }
  }

  @Test
  public void testBalance() {
    System.out.println("");
    System.out.println("Balance test");
    try {
      String singleAddress = wallet.createAddress(userKey);
      System.out.println("Single Address: " + singleAddress);
      String multiSigAddress = wallet.getMultiSigAddress(Arrays.asList(singleAddress), userKey);
      System.out.println("Multi-sig Address: " + multiSigAddress);

      String balance = wallet.getBalance(singleAddress);
      System.out.println("Balance for " + singleAddress + ": " + balance);
      balance = wallet.getBalance(multiSigAddress);
      System.out.println("Balance for " + multiSigAddress + ": " + balance);
    } catch (Exception e) {
      LOGGER.error(null, e);
      fail("Balance test failed");
    }
  }
}
