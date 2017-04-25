package io.emax.cosigner.bitcoin;

import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.bitcoin.stubrpc.BitcoinTestRpc;
import io.emax.cosigner.common.Json;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TransactionTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(WalletTest.class);
  private BitcoinWallet wallet;
  private String userKey;

  @Before
  public void setUp() {
    BitcoinResource.getResource().setBitcoindRpc(new BitcoinTestRpc());
    wallet = new BitcoinWallet(new BitcoinConfiguration());
    userKey = "deadbeef";
  }

  @Test
  public void testTransactionCreationAndSigning() {
    String userKey1 = "deadbeef";
    String userKey2 = "deadbeef1234";
    String address1 = wallet.createAddress(userKey1);
    String address2 = wallet.createAddress(userKey2);
    address2 = wallet.getMultiSigAddress(Collections.singletonList(address2), userKey2);
    Wallet.Recipient recipient = new Wallet.Recipient();
    recipient.setAmount(BigDecimal.valueOf(29.1));
    recipient.setRecipientAddress(address2);

    String transaction = wallet.createTransaction(Collections.singletonList(address1),
        Collections.singletonList(recipient));
    String feeTransaction = wallet.createTransaction(Collections.singletonList(address1),
        Collections.singletonList(recipient), "[\"includeFees\"]");

    // Test RPC generates an output for deadbeef key, only thing that will get signed.
    String signedTransaction = wallet.signTransaction(transaction, address1, userKey1);
    System.out.println(transaction);
    System.out.println(signedTransaction);
    Assert
        .assertTrue("Transaction was not signed", !signedTransaction.equalsIgnoreCase(transaction));

    // TODO We should be verifying the outputs for balance + change.
    signedTransaction = wallet.signTransaction(feeTransaction, address1, userKey1);
    System.out.println(feeTransaction);
    System.out.println(signedTransaction);
    Assert
        .assertTrue("Transaction was not signed", !signedTransaction.equalsIgnoreCase(transaction));

    Iterable<String> signers = wallet.getSignersForTransaction(transaction);
    String signer = signers.iterator().next();
    Assert.assertTrue("Unexpected signer!", signer.equalsIgnoreCase(address1));

    // Attempt to sign with a multi-sig address even though it should fail (no signers).
    wallet.signTransaction(transaction, address2, userKey2);
    // Attempt to sign with multi-sig keys even though they should fail (no signers).
    wallet.signTransaction(transaction, address2);

    // Attempt to decode our transaction, expect the value of the input.
    Wallet.TransactionDetails tx = wallet.decodeRawTransaction(transaction);
    Assert.assertTrue("TX value not what we expected",
        tx.getAmount().compareTo(BigDecimal.valueOf(30)) == 0);
  }

  @Test
  public void testTransactionLookup() {
    String userKey1 = "deadbeef";
    String address1 = wallet.createAddress(userKey1);

    Wallet.TransactionDetails[] txs = wallet.getTransactions(address1, 100, 0);
    LOGGER.debug(Json.stringifyObject(List.class, Arrays.asList(txs)));
    Assert.assertTrue("No txs found!", txs.length > 0);

    // Expecting that parsing it doesn't throw any exceptions.
    Wallet.TransactionDetails tx = wallet.getTransaction("deadbeef");
  }

}
