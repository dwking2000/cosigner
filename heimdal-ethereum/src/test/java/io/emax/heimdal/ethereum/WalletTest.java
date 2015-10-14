package io.emax.heimdal.ethereum;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.Test;

import io.emax.heimdal.api.currency.Wallet.Recipient;
import io.emax.heimdal.ethereum.common.ByteUtilities;
import io.emax.heimdal.ethereum.common.DeterministicTools;
import io.emax.heimdal.ethereum.common.Secp256k1;
import io.emax.heimdal.ethereum.gethrpc.RawTransaction;
import io.emax.heimdal.ethereum.stubrpc.EthereumTestRpc;
import junit.framework.TestCase;

public class WalletTest extends TestCase {
  private static Wallet wallet;
  private static String userKey;

  @Override
  public void setUp() {
    wallet = new Wallet(new EthereumTestRpc());
    userKey = "deadbeef";
  }

  @Test
  public void testWallet() {
    System.out.println("");
    System.out.println("Simple operation test, should be no exceptions");

    try {
      // Get a new address
      String singleAddress = wallet.createAddress(userKey);
      System.out.println("Single Address Test: " + singleAddress);
      String multiAddress = wallet.getMultiSigAddress(Arrays.asList(singleAddress), userKey);
      System.out.println("Multi Address Test: " + multiAddress);

      // Generating a second set of addresses
      singleAddress = wallet.createAddress(userKey);
      System.out.println("Single Address Test: " + singleAddress);
      multiAddress = wallet.getMultiSigAddress(Arrays.asList(singleAddress), userKey);
      System.out.println("Multi Address Test: " + multiAddress);

      // Send some money
      Recipient recipient = new Recipient();
      recipient.setAmount(BigDecimal.valueOf(50));
      recipient.setRecipientAddress(multiAddress);
      String txString =
          wallet.createTransaction(Arrays.asList(singleAddress), Arrays.asList(recipient));
      System.out
          .println("50 ether from " + singleAddress + " to " + multiAddress + ": " + txString);
      txString = wallet.signTransaction(txString, singleAddress, userKey);
      System.out.println("Signed TX: " + txString);
      txString = wallet.sendTransaction(txString);
      System.out.println("Tx ID: " + txString);

      // Send some money from the multi-sig to create the alternate tx
      recipient = new Recipient();
      recipient.setAmount(BigDecimal.valueOf(25.3));
      recipient.setRecipientAddress(singleAddress);
      txString = wallet.createTransaction(Arrays.asList(multiAddress), Arrays.asList(recipient));
      System.out
          .println("25.3 ether from " + multiAddress + " to " + singleAddress + ": " + txString);
      txString = wallet.signTransaction(txString, multiAddress, userKey);
      System.out.println("Signed TX: " + txString);
      txString = wallet.sendTransaction(txString);
      System.out.println("Tx ID: " + txString);

      String balance = wallet.getBalance(multiAddress);
      System.out.println("Balance of " + multiAddress + ": " + balance);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Simple operation test failed!");
      return;
    }
  }

  @Test
  public void testNoPrivateKey() {
    System.out.println("");
    System.out.println("Testing signature with no private key available");
    try {
      // Get a new address
      String singleAddress = wallet.createAddress(userKey);
      System.out.println("Single Address Test: " + singleAddress);
      String multiAddress = wallet.getMultiSigAddress(Arrays.asList(singleAddress), userKey);
      System.out.println("Multi Address Test: " + multiAddress);

      // Send some money from the multi-sig to create the alternate tx
      Recipient recipient = new Recipient();
      recipient.setAmount(BigDecimal.valueOf(25.3));
      recipient.setRecipientAddress(singleAddress);
      String txString =
          wallet.createTransaction(Arrays.asList(multiAddress), Arrays.asList(recipient));
      System.out
          .println("25.3 ether from " + multiAddress + " to " + singleAddress + ": " + txString);
      String signedTxString = wallet.signTransaction(txString, multiAddress, "bad" + userKey);
      System.out.println("Signed TX: " + signedTxString);

      // If the key is missing the result should be the same TX string we submitted
      assertEquals(txString, signedTxString);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Missing key test should not throw exceptions!");
      return;
    }
  }

  @Test
  public void testValidPrivateKey() {
    System.out.println("");
    System.out.println("Testing signature with valid private key");
    try {
      // Get a new address
      String singleAddress = wallet.createAddress(userKey);
      System.out.println("Single Address Test: " + singleAddress);
      String multiAddress = wallet.getMultiSigAddress(Arrays.asList(singleAddress), userKey);
      System.out.println("Multi Address Test: " + multiAddress);

      // Send some money from the multi-sig to create the alternate tx
      Recipient recipient = new Recipient();
      recipient.setAmount(BigDecimal.valueOf(25.3));
      recipient.setRecipientAddress(singleAddress);
      String txString =
          wallet.createTransaction(Arrays.asList(multiAddress), Arrays.asList(recipient));
      System.out
          .println("25.3 ether from " + multiAddress + " to " + singleAddress + ": " + txString);
      String signedTxString = wallet.signTransaction(txString, multiAddress, userKey);
      System.out.println("Signed TX: " + signedTxString);

      // If the key is missing the result should be the same TX string we submitted, we want the
      // signature data to be there too
      assertTrue(!txString.equalsIgnoreCase(signedTxString));

      // See if we can recover the signing key...
      RawTransaction txStructure =
          RawTransaction.parseBytes(ByteUtilities.toByteArray(signedTxString));

      byte[] sigBytes = txStructure.getSigBytes();
      String sigBytesString = ByteUtilities.toHexString(sigBytes);
      sigBytesString = DeterministicTools.hashSha3(sigBytesString);
      sigBytes = ByteUtilities.toByteArray(sigBytesString);

      String signingAddress = ByteUtilities.toHexString(Secp256k1.recoverPublicKey(
          txStructure.getSigR().getDecodedContents(), txStructure.getSigS().getDecodedContents(),
          sigBytes, txStructure.getSigV().getDecodedContents()[0] - 27));
      signingAddress = DeterministicTools.getPublicAddress(signingAddress, false);
      System.out.println("Signed by: " + signingAddress);

      assertEquals(signingAddress, singleAddress);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Missing key test should not throw exceptions!");
      return;
    }
  }

  @Test
  public void testPreExistingAddresses() {
    System.out.println("");
    System.out.println("Testing address load");

    boolean foundPreExisting = false;
    for (String address : wallet.getAddresses(userKey)) {
      System.out.println(address);
      foundPreExisting = true;
    }

    assertTrue(foundPreExisting);
  }
}
