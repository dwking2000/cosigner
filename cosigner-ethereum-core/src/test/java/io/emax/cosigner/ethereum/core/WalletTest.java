package io.emax.cosigner.ethereum.core;

import io.emax.cosigner.api.currency.Wallet.Recipient;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.common.crypto.Secp256k1;
import io.emax.cosigner.ethereum.core.common.EthereumTools;
import io.emax.cosigner.ethereum.core.gethrpc.RawTransaction;
import io.emax.cosigner.ethereum.core.stubrpc.EthereumTestRpc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WalletTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(WalletTest.class);
  private EthereumWallet wallet;
  private String userKey;

  @Before
  public void setUp() {
    EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
    wallet = new EthereumWallet(new EthereumConfiguration());
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
      String multiAddress =
          wallet.getMultiSigAddress(Collections.singletonList(singleAddress), userKey);
      System.out.println("Multi Address Test: " + multiAddress);

      // Generating a second set of addresses
      singleAddress = wallet.createAddress(userKey);
      System.out.println("Single Address Test: " + singleAddress);
      multiAddress = wallet.getMultiSigAddress(Collections.singletonList(singleAddress), userKey);
      System.out.println("Multi Address Test: " + multiAddress);

      // Send some money
      Recipient recipient = new Recipient();
      recipient.setAmount(BigDecimal.valueOf(50));
      recipient.setRecipientAddress(multiAddress);
      String txString = wallet.createTransaction(Collections.singletonList(singleAddress),
          Collections.singletonList(recipient));
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
      txString = wallet.createTransaction(Collections.singletonList(multiAddress),
          Collections.singletonList(recipient));
      System.out
          .println("25.3 ether from " + multiAddress + " to " + singleAddress + ": " + txString);
      txString = wallet.signTransaction(txString, multiAddress, userKey);
      System.out.println("Signed TX: " + txString);
      txString = wallet.sendTransaction(txString);
      System.out.println("Tx ID: " + txString);

      String balance = wallet.getBalance(multiAddress);
      System.out.println("Balance of " + multiAddress + ": " + balance);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Simple operation test failed!");
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
      String multiAddress =
          wallet.getMultiSigAddress(Collections.singletonList(singleAddress), userKey);
      System.out.println("Multi Address Test: " + multiAddress);

      // Send some money from the multi-sig to create the alternate tx
      Recipient recipient = new Recipient();
      recipient.setAmount(BigDecimal.valueOf(25.3));
      recipient.setRecipientAddress(singleAddress);
      String txString = wallet.createTransaction(Collections.singletonList(multiAddress),
          Collections.singletonList(recipient));
      System.out
          .println("25.3 ether from " + multiAddress + " to " + singleAddress + ": " + txString);
      String signedTxString = wallet.signTransaction(txString, multiAddress, "bad" + userKey);
      System.out.println("Signed TX: " + signedTxString);

      // If the key is missing the result should be the same TX string we submitted
      Assert.assertEquals(txString, signedTxString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Missing key test should not throw exceptions!");
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
      String multiAddress =
          wallet.getMultiSigAddress(Collections.singletonList(singleAddress), userKey);
      System.out.println("Multi Address Test: " + multiAddress);

      // Send some money from the multi-sig to create the alternate tx
      Recipient recipient = new Recipient();
      recipient.setAmount(BigDecimal.valueOf(25.3));
      recipient.setRecipientAddress(singleAddress);
      String txString = wallet.createTransaction(Collections.singletonList(multiAddress),
          Collections.singletonList(recipient));
      System.out
          .println("25.3 ether from " + multiAddress + " to " + singleAddress + ": " + txString);
      String signedTxString = wallet.signTransaction(txString, multiAddress, userKey);
      System.out.println("Signed TX: " + signedTxString);

      // If the key is missing the result should be the same TX string we submitted, we want the
      // signature data to be there too
      Assert.assertTrue(!txString.equalsIgnoreCase(signedTxString));

      // See if we can recover the signing key...
      RawTransaction txStructure =
          RawTransaction.parseBytes(ByteUtilities.toByteArray(signedTxString));

      byte[] sigBytes = txStructure.getSigBytes();
      String sigBytesString = ByteUtilities.toHexString(sigBytes);
      sigBytesString = EthereumTools.hashKeccak(sigBytesString);
      sigBytes = ByteUtilities.toByteArray(sigBytesString);

      String signingAddress = ByteUtilities.toHexString(Secp256k1
          .recoverPublicKey(txStructure.getSigR().getDecodedContents(),
              txStructure.getSigS().getDecodedContents(),
              new byte[]{(byte) (txStructure.getSigV().getDecodedContents()[0] - 27)}, sigBytes))
          .substring(2);
      signingAddress = EthereumTools.getPublicAddress(signingAddress, false);
      System.out.println("Signed by: " + signingAddress);

      Assert.assertEquals(signingAddress, singleAddress);

    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Missing key test should not throw exceptions!");
    }
  }

  @Test
  public void testPreExistingAddresses() {
    System.out.println("");
    System.out.println("Testing address load");

    testWallet();

    boolean foundPreExisting = false;
    for (String address : wallet.getAddresses(userKey)) {
      System.out.println(address);
      foundPreExisting = true;
    }

    Assert.assertTrue(foundPreExisting);
  }

  @Test
  public void testHashing() {
    System.out.println("");
    System.out.println("Testing that Keccak hash is still working properly.");

    String expected = "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470";
    String tested = EthereumTools.hashKeccak("");

    Assert.assertEquals(expected, tested);
  }

  @Test
  public void testTransactionLookup() {
    try {
      String singleAddress = wallet.createAddress(userKey);
      String multiAddress =
          wallet.getMultiSigAddress(Collections.singletonList(singleAddress), userKey);
      System.out.println(Json.stringifyObject(List.class,
          Arrays.asList(wallet.getTransactions(multiAddress, 100, 0))));

      System.out.println(wallet.getTransaction("deadbeef"));
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Problem testing transaction lookup");
    }
  }
}
