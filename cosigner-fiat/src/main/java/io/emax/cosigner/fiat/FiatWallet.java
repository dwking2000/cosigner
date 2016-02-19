package io.emax.cosigner.fiat;

import io.emax.cosigner.api.core.ServerStatus;
import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.crypto.Secp256k1;
import io.emax.cosigner.ethereum.EthereumResource;
import io.emax.cosigner.ethereum.common.EthereumTools;
import io.emax.cosigner.ethereum.gethrpc.EthereumRpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Locale;

public class FiatWallet implements Wallet {
  private static final Logger LOGGER = LoggerFactory.getLogger(FiatWallet.class);

  // RPC and configuration
  private static final EthereumRpc ethereumRpc = EthereumResource.getResource().getGethRpc();
  private static final FiatConfiguration config = new FiatConfiguration();

  private HashSet<String> knownAddresses = new HashSet<>();

  // TODO Need a method that scans for contracts on the main account and finds any existing fiat contract automatically.
  //      This should take a currency symbol and pick up the one we're after.
  //      I.E. Cosigner registers a FiatWallet("EUR"), which does a scan(),
  //           and either creates a new contract, or uses the existing contract if it exists.
  // TODO Consider an auto-upgrade process on a new contract version. Probably not needed though.
  // TODO Make it possible to specify the contract address, we should know what it is.
  // TODO Make an option to create the contract if one doesn't exist, for first run, etc...

  @Override
  public String createAddress(String name) {
    return createAddress(name, 0);
  }

  @Override
  public String createAddress(String name, int skipNumber) {
    // Generate the next private key
    LOGGER.debug("Creating a new normal address...");
    int rounds = 1 + skipNumber;
    String privateKey =
        EthereumTools.getDeterministicPrivateKey(name, config.getServerKey(), rounds);

    // Convert to an Ethereum address
    String publicAddress = EthereumTools.getPublicAddress(privateKey);

    while (knownAddresses.contains(publicAddress.toLowerCase(Locale.US))) {
      rounds++;
      privateKey = EthereumTools.getDeterministicPrivateKey(name, config.getServerKey(), rounds);
      publicAddress = EthereumTools.getPublicAddress(privateKey);
    }
    knownAddresses.add(publicAddress);

    LOGGER.debug("New address " + publicAddress + " generated after " + rounds + " rounds");
    return publicAddress;
  }

  @Override
  public boolean registerAddress(String address) {
    return true;
  }

  @Override
  public String createAddressFromKey(String key, boolean isPrivateKey) {
    return EthereumTools.getPublicAddress(key, isPrivateKey);
  }

  @Override
  public Iterable<String> getAddresses(String name) {
    // TODO Loop through addresses from name while they exist in the knownAddresses, I.E. those that have funds.
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
    // TODO Lookup balance in the contract.
    return null;
  }

  @Override
  public String createTransaction(Iterable<String> fromAddresses, Iterable<Recipient> toAddresses) {
    // TODO Create a trading transaction for user-address balance
    return null;
  }

  @Override
  public Iterable<String> getSignersForTransaction(String transaction) {
    // TODO Essentially only care about the sender here.
    return null;
  }

  @Override
  public String signTransaction(String transaction, String address) {
    // TODO
    return null;
  }

  @Override
  public String signTransaction(String transaction, String address, String name) {
    // TODO
    return null;
  }

  @Override
  public Iterable<Iterable<String>> getSigString(String transaction, String address) {
    // TODO
    return null;
  }

  @Override
  public String applySignature(String transaction, String address,
      Iterable<Iterable<String>> signatureData) {
    // TODO
    return null;
  }

  @Override
  public String sendTransaction(String transaction) {
    // TODO We may want to re-sign like in the base Ethereum msig contract. We'll see.
    return null;
  }

  @Override
  public TransactionDetails[] getTransactions(String address, int numberToReturn, int skipNumber) {
    // TODO
    return new TransactionDetails[0];
  }

  @Override
  public ServerStatus getWalletStatus() {
    try {
      ethereumRpc.eth_blockNumber();
      return ServerStatus.CONNECTED;
    } catch (Exception e) {
      return ServerStatus.DISCONNECTED;
    }
  }

  @Override
  public String generatePrivateKey() {
    return ByteUtilities.toHexString(Secp256k1.generatePrivateKey());
  }

  @Override
  public String generatePublicKey(String privateKey) {
    return EthereumTools.getPublicKey(privateKey);
  }

  @Override
  public Iterable<Iterable<String>> signWithPrivateKey(Iterable<Iterable<String>> data,
      String privateKey) {
    // TODO
    return null;
  }

  // TODO Add some admin functions for creation and destruction of tokens. Figure out where we're going to put these in tooling.
}
