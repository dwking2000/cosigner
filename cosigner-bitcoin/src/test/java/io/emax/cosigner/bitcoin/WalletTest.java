package io.emax.cosigner.bitcoin;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.Test;

import io.emax.cosigner.api.currency.Wallet.Recipient;
import io.emax.cosigner.bitcoin.stubrpc.BitcoinTestRpc;
import junit.framework.TestCase;

public class WalletTest extends TestCase {
  private static BitcoinWallet wallet;
  private static String userKey;

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
      String singleAddress = wallet.createAddress(userKey);
      System.out.println("Single Address: " + singleAddress);
      String multiSigAddress = wallet.getMultiSigAddress(Arrays.asList(singleAddress), userKey);
      System.out.println("Multi-sig Address: " + multiSigAddress);

      Recipient recipient = new Recipient();
      recipient.setAmount(BigDecimal.valueOf(20));
      recipient.setRecipientAddress(multiSigAddress);
      String txString =
          wallet.createTransaction(Arrays.asList(singleAddress), Arrays.asList(recipient));
      System.out
          .println("20 BTC from " + singleAddress + " to " + multiSigAddress + ": " + txString);
      txString = wallet.signTransaction(txString, singleAddress, userKey);
      System.out.println("Signed TX: " + txString);
      txString = wallet.sendTransaction(txString);
      System.out.println("TX ID: " + txString);
    } catch (Exception e) {
      e.printStackTrace(System.err);
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
      e.printStackTrace(System.err);
      fail("Balance test failed");
    }
  }
}
