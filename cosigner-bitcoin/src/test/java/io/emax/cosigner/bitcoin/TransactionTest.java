package io.emax.cosigner.bitcoin;

import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.bitcoin.stubrpc.BitcoinTestRpc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collections;

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
    address1 = wallet.getMultiSigAddress(Collections.singletonList(address1), userKey1);
    String address2 = wallet.createAddress(userKey2);
    address2 = wallet.getMultiSigAddress(Collections.singletonList(address2), userKey2);
    Wallet.Recipient recipient = new Wallet.Recipient();
    recipient.setAmount(BigDecimal.valueOf(29.97));
    recipient.setRecipientAddress(address2);

    String transaction = wallet.createTransaction(Collections.singletonList(address1),
        Collections.singletonList(recipient));

    String signedTransaction = wallet.signTransaction(transaction, address1, userKey1);
    System.out.println(transaction);
    System.out.println(signedTransaction);
    //Assert.assertTrue("Transaction was not signed", !signedTransaction.equalsIgnoreCase(transaction));
  }

  @Test
  public void testTransactionLookup() {

  }

}
