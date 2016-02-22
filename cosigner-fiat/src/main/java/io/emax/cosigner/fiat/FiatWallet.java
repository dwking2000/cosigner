package io.emax.cosigner.fiat;

import io.emax.cosigner.api.core.ServerStatus;
import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.crypto.Secp256k1;
import io.emax.cosigner.ethereum.EthereumResource;
import io.emax.cosigner.ethereum.common.EthereumTools;
import io.emax.cosigner.ethereum.common.RlpItem;
import io.emax.cosigner.ethereum.common.RlpList;
import io.emax.cosigner.ethereum.gethrpc.CallData;
import io.emax.cosigner.ethereum.gethrpc.DefaultBlock;
import io.emax.cosigner.ethereum.gethrpc.EthereumRpc;
import io.emax.cosigner.ethereum.gethrpc.RawTransaction;
import io.emax.cosigner.fiat.gethrpc.FiatContract.FiatContract;
import io.emax.cosigner.fiat.gethrpc.FiatContract.FiatContractInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class FiatWallet implements Wallet {
  private static final Logger LOGGER = LoggerFactory.getLogger(FiatWallet.class);

  // RPC and configuration
  private static final EthereumRpc ethereumRpc = EthereumResource.getResource().getGethRpc();
  private static FiatConfiguration config;

  private String contractAddress = "";
  private FiatContractInterface contractInterface = new FiatContract();
  private HashSet<String> knownAddresses = new HashSet<>();
  private HashMap<String, HashSet<String>> ownedAddresses = new HashMap<>();

  public FiatWallet(String currency) {
    config = new FiatConfiguration(currency);
    setupFiatContract();
  }

  private void setupFiatContract() {
    if (config.getContractAddress() != null && !config.getContractAddress().isEmpty()) {
      contractAddress = config.getContractAddress();
      contractInterface = getContractType(contractAddress);
    } else {
      try {
        String txCount = ethereumRpc.eth_getTransactionCount("0x" + config.getContractAccount(),
            DefaultBlock.LATEST.toString());
        int rounds = new BigInteger(1, ByteUtilities.toByteArray(txCount)).intValue();
        for (int i = 0; i < rounds; i++) {
          RlpList contractAddress = new RlpList();
          RlpItem contractCreator =
              new RlpItem(ByteUtilities.toByteArray(config.getContractAccount()));
          RlpItem nonce =
              new RlpItem(ByteUtilities.stripLeadingNullBytes(BigInteger.valueOf(i).toByteArray()));
          contractAddress.add(contractCreator);
          contractAddress.add(nonce);

          String contract =
              EthereumTools.hashKeccak(ByteUtilities.toHexString(contractAddress.encode()))
                  .substring(96 / 4, 256 / 4);
          FiatContractInterface contractClass = getContractType(contract);
          if (contractClass != null) {
            this.contractInterface = contractClass;
            this.contractAddress = contract;
            break;
          }
        }

      } catch (Exception e) {
        LOGGER.debug(null, e);
      }
    }

    if ((contractAddress == null || contractAddress.isEmpty()) && config.generateNewContract()) {
      LOGGER.debug("Generating new contract...");
      // Create the TX data structure
      RawTransaction tx = new RawTransaction();
      tx.getGasPrice().setDecodedContents(ByteUtilities
          .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
      tx.getGasLimit().setDecodedContents(ByteUtilities
          .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));
      tx.getData().setDecodedContents(ByteUtilities.toByteArray(
          contractInterface.getContractParameters()
              .createContract(config.getAdminAccount(), Arrays.asList(config.getMultiSigAccounts()),
                  config.getMinSignatures())));

      String rawTx = ByteUtilities.toHexString(tx.encode());
      LOGGER.debug("Creating contract: " + rawTx);
      // TODO Sign and send it.
    }
  }

  private FiatContractInterface getContractType(String contract) {
    try {
      String contractCode = ethereumRpc
          .eth_getCode("0x" + contract.toLowerCase(Locale.US), DefaultBlock.LATEST.toString());
      contractCode = contractCode.substring(2);
      LOGGER.debug("Contract code: " + contractCode);
      Class<?> contractType = FiatContract.class;
      while (FiatContractInterface.class.isAssignableFrom(contractType)) {
        FiatContractInterface contractParams = (FiatContractInterface) contractType.newInstance();
        if (contractParams.getContractPayload().equalsIgnoreCase(contractCode)) {
          return contractParams;
        }
        contractType = contractType.getSuperclass();
      }
    } catch (Exception e) {
      LOGGER.debug(null, e);
      return null;
    }
    return null;
  }

  @Override
  public String createAddress(String name) {
    return createAddress(name, 0);
  }

  @Override
  public String createAddress(String name, int skipNumber) {
    // Generate the next private key
    LOGGER.debug("Creating a new normal address...");
    String user = EthereumTools.encodeUserKey(name);
    if (!ownedAddresses.containsKey(user)) {
      getAddresses(name);
    }

    int rounds = 1 + skipNumber;
    String privateKey =
        EthereumTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);

    // Convert to an Ethereum address
    String publicAddress = EthereumTools.getPublicAddress(privateKey);

    while (knownAddresses.contains(publicAddress.toLowerCase(Locale.US))) {
      rounds++;
      privateKey =
          EthereumTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);
      publicAddress = EthereumTools.getPublicAddress(privateKey);
    }
    knownAddresses.add(publicAddress);

    ownedAddresses.get(user).add(publicAddress);

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
    String user = EthereumTools.encodeUserKey(name);
    if (!ownedAddresses.containsKey(user)) {
      ownedAddresses.put(user, new HashSet<>());
    }

    HashSet<String> userAddresses = ownedAddresses.get(user);
    if (userAddresses.isEmpty()) {
      String balanceCheck = getBalance(createAddress(name));
      while (balanceCheck != null && balanceCheck != BigDecimal.ZERO.toPlainString()) {
        userAddresses = ownedAddresses.get(user);
        balanceCheck = getBalance(createAddress(name));
      }
    }

    return userAddresses;
  }

  @Override
  public String getMultiSigAddress(Iterable<String> addresses, String name) {
    return addresses.iterator().next();
  }

  @Override
  public String getBalance(String address) {
    CallData callData = new CallData();
    callData.setTo("0x" + contractAddress);
    callData.setData("0x" + contractInterface.getContractParameters().getBalance(address));
    callData.setGas("100000"); // Doesn't matter, just can't be nil
    callData.setGasPrice("100000"); // Doesn't matter, just can't be nil
    String response = ethereumRpc.eth_call(callData, DefaultBlock.LATEST.toString());

    BigInteger balance = new BigInteger(1, ByteUtilities.toByteArray(response));
    return balance.toString(10);
  }

  @Override
  public String createTransaction(Iterable<String> fromAddresses, Iterable<Recipient> toAddresses) {
    // Create the TX data structure
    RawTransaction tx = new RawTransaction();
    tx.getTo().setDecodedContents(ByteUtilities.toByteArray(contractAddress));
    tx.getGasPrice().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
    tx.getGasLimit().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));

    List<String> recipients = new LinkedList<>();
    List<Long> amounts = new LinkedList<>();
    toAddresses.forEach(recipient -> {
      amounts.add(recipient.getAmount().longValue());
      recipients.add(recipient.getRecipientAddress());
    });

    // TODO Lookup nonce.

    tx.getData().setDecodedContents(ByteUtilities.toByteArray(
        contractInterface.getContractParameters()
            .transfer(0L, fromAddresses.iterator().next(), recipients, amounts, new LinkedList<>(),
                new LinkedList<>(), new LinkedList<>())));

    return ByteUtilities.toHexString(tx.encode());
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
