package io.emax.cosigner.fiat;

import io.emax.cosigner.api.core.ServerStatus;
import io.emax.cosigner.api.currency.Wallet;

public class FiatWallet implements Wallet {
  @Override
  public String createAddress(String name) {
    // TODO Generate a regular Ethereum address
    return null;
  }

  @Override
  public String createAddress(String name, int skipNumber) {
    return null;
  }

  @Override
  public boolean registerAddress(String address) {
    return true;
  }

  @Override
  public String createAddressFromKey(String key, boolean isPrivateKey) {
    return null;
  }

  @Override
  public Iterable<String> getAddresses(String name) {
    return null;
  }

  @Override
  public String getMultiSigAddress(Iterable<String> addresses, String name) {
    // TODO Due to the nature of this contract, this doesn't do anything. Just return the single-key address.
    // The contract is multi-sig protected, but there's only one of them. So user-address is the differentiator.
    return null;
  }

  @Override
  public String getBalance(String address) {
    return null;
  }

  @Override
  public String createTransaction(Iterable<String> fromAddresses, Iterable<Recipient> toAddresses) {
    // TODO Create a trading transaction for user-address balance
    return null;
  }

  @Override
  public Iterable<String> getSignersForTransaction(String transaction) {
    return null;
  }

  @Override
  public String signTransaction(String transaction, String address) {
    return null;
  }

  @Override
  public String signTransaction(String transaction, String address, String name) {
    return null;
  }

  @Override
  public Iterable<Iterable<String>> getSigString(String transaction, String address) {
    return null;
  }

  @Override
  public String applySignature(String transaction, String address,
      Iterable<Iterable<String>> signatureData) {
    return null;
  }

  @Override
  public String sendTransaction(String transaction) {
    return null;
  }

  @Override
  public TransactionDetails[] getTransactions(String address, int numberToReturn, int skipNumber) {
    return new TransactionDetails[0];
  }

  @Override
  public ServerStatus getWalletStatus() {
    return null;
  }

  @Override
  public String generatePrivateKey() {
    return null;
  }

  @Override
  public String generatePublicKey(String privateKey) {
    return null;
  }

  @Override
  public Iterable<Iterable<String>> signWithPrivateKey(Iterable<Iterable<String>> data,
      String privateKey) {
    return null;
  }

  // TODO Add some admin functions for creation and destruction of tokens. Figure out where we're going to put these in tooling.
}
