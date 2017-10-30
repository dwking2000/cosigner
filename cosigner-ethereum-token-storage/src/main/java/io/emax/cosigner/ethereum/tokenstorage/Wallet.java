package io.emax.cosigner.ethereum.tokenstorage;

import io.emax.cosigner.api.core.ServerStatus;
import io.emax.cosigner.api.currency.CurrencyAdmin;
import io.emax.cosigner.api.currency.OfflineWallet;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.common.crypto.Secp256k1;
import io.emax.cosigner.ethereum.core.EthereumConfiguration;
import io.emax.cosigner.ethereum.core.common.EthereumTools;
import io.emax.cosigner.ethereum.core.gethrpc.Block;
import io.emax.cosigner.ethereum.core.gethrpc.CallData;
import io.emax.cosigner.ethereum.core.gethrpc.DefaultBlock;
import io.emax.cosigner.ethereum.core.gethrpc.RawTransaction;
import io.emax.cosigner.ethereum.tokenstorage.contract.ContractInterface;
import io.emax.cosigner.ethereum.tokenstorage.contract.ContractParametersInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static io.emax.cosigner.ethereum.tokenstorage.Base.ethereumRpc;
import static io.emax.cosigner.ethereum.tokenstorage.Utilities.getContractVersion;

/**
 * Implementation of the Wallet API for Ethereum-based ERC-20 Tokens.
 */
public class Wallet implements io.emax.cosigner.api.currency.Wallet, OfflineWallet, CurrencyAdmin {
  private static final Logger LOGGER = LoggerFactory.getLogger(Wallet.class);

  Configuration config;
  HashSet<String> knownAddresses = new HashSet<>();
  HashMap<String, HashSet<String>> ownedAddresses = new HashMap<>();

  public Wallet(Configuration conf) {
    config = conf;
    Setup.setupTokenContract(config);
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
      LOGGER.debug("Address " + publicAddress + " already known");
      LOGGER.debug("KnownAddress: " + Json.stringifyObject(Set.class, knownAddresses));
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
      while (balanceCheck != null
          && (new BigDecimal(balanceCheck).compareTo(BigDecimal.ZERO)) != 0) {
        LOGGER.debug(
            "BalanceCheck was: " + balanceCheck + " compared to " + BigInteger.ZERO.toString(10));
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
    CallData callData = EthereumTools
        .generateCall(config.getContractInterface().getContractParameters().getBalance(address),
            config.getStorageContractAddress());
    LOGGER.debug("Balance request: " + Json.stringifyObject(CallData.class, callData));
    String response = ethereumRpc.eth_call(callData, DefaultBlock.LATEST.toString());

    BigInteger intBalance = new BigInteger(1, ByteUtilities.toByteArray(response));
    BigDecimal balance = new BigDecimal(intBalance);

    balance = balance.setScale(20, BigDecimal.ROUND_UNNECESSARY);
    balance = balance.divide(BigDecimal.valueOf(10).pow((int) config.getDecimalPlaces()),
        BigDecimal.ROUND_UNNECESSARY);

    // Subtract any pending txs from the available balance
    TransactionDetails[] txDetails = getTransactions(address, 100, 0);
    for (TransactionDetails txDetail : txDetails) {
      if (!txDetail.isConfirmed() && txDetail.getToAddress()[0].equalsIgnoreCase(address)) {
        balance = balance.subtract(txDetail.getAmount());
      }
    }
    return balance.toPlainString();
  }

  @Override
  public String getPendingBalance(String address) {
    BigDecimal balance = BigDecimal.ZERO;
    TransactionDetails[] txDetails = getTransactions(address, 100, 0);
    for (TransactionDetails txDetail : txDetails) {
      if (!txDetail.isConfirmed() && txDetail.getToAddress()[0].equalsIgnoreCase(address)) {
        balance = balance.add(txDetail.getAmount());
      } else if (!txDetail.isConfirmed() && txDetail.getFromAddress()[0]
          .equalsIgnoreCase(address)) {
        balance = balance.subtract(txDetail.getAmount());
      }
    }
    balance = balance.max(BigDecimal.ZERO);
    return balance.toPlainString();
  }

  public String getTotalBalances() {
    CallData callData = EthereumTools
        .generateCall(config.getContractInterface().getContractParameters().getTotalBalance(),
            config.getStorageContractAddress());
    LOGGER.debug("Total balance request: " + Json.stringifyObject(CallData.class, callData));
    String response = ethereumRpc.eth_call(callData, DefaultBlock.LATEST.toString());

    BigInteger intBalance = new BigInteger(1, ByteUtilities.toByteArray(response));
    BigDecimal balance = new BigDecimal(intBalance);

    balance = balance.setScale(20, BigDecimal.ROUND_UNNECESSARY);
    return balance.divide(BigDecimal.valueOf(10).pow((int) config.getDecimalPlaces()),
        BigDecimal.ROUND_UNNECESSARY).toPlainString();
  }

  @Override
  public String createTransaction(Iterable<String> fromAddresses, Iterable<Recipient> toAddresses) {
    return createTransaction(fromAddresses, toAddresses, null);
  }

  @Override
  public String createTransaction(Iterable<String> fromAddresses, Iterable<Recipient> toAddresses,
      String options) {
    String firstSender =
        ByteUtilities.toHexString(ByteUtilities.toByteArray(fromAddresses.iterator().next()));
    String contract = config.getStorageContractAddress();
    ContractInterface txInterface = getContractVersion(firstSender, config);
    if (txInterface != null && config.useTokenTransferFunction()) {
      contract = firstSender;
    }
    if (txInterface == null && config.useTokenTransferFunction()) {
      Recipient recipient = toAddresses.iterator().next();

      String rcpt =
          ByteUtilities.toHexString(ByteUtilities.toByteArray(recipient.getRecipientAddress()));
      BigInteger amount =
          recipient.getAmount().multiply(BigDecimal.TEN.pow((int) config.getDecimalPlaces()))
              .toBigInteger();

      RawTransaction tx = RawTransaction
          .createTransaction(config, config.getTokenContractAddress(), null,
              config.getContractInterface().getContractParameters().tokenTransfer(rcpt, amount));

      return ByteUtilities.toHexString(tx.encode());
    } else {
      if (txInterface == null) {
        txInterface = config.getContractInterface();
      }
      // Format tx data
      List<String> recipients = new LinkedList<>();
      List<BigInteger> amounts = new LinkedList<>();
      toAddresses.forEach(recipient -> {
        amounts.add(
            recipient.getAmount().multiply(BigDecimal.TEN.pow((int) config.getDecimalPlaces()))
                .toBigInteger());
        recipients.add(
            ByteUtilities.toHexString(ByteUtilities.toByteArray(recipient.getRecipientAddress())));
      });

      String txCount = ethereumRpc.eth_getStorageAt("0x" + contract.toLowerCase(Locale.US), "0x1",
          DefaultBlock.LATEST.toString());
      BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount)).add(BigInteger.ONE);

      // Create the TX data structure
      RawTransaction tx = RawTransaction.createTransaction(config, contract, null,
          txInterface.getContractParameters()
              .offlineTransfer(nonce.longValue(), firstSender, recipients, amounts,
                  new LinkedList<>(), new LinkedList<>(), new LinkedList<>()));

      return ByteUtilities.toHexString(tx.encode());
    }
  }

  @Override
  public Iterable<String> getSignersForTransaction(String transaction) {
    RawTransaction rawTx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    if (rawTx == null) {
      return new LinkedList<>();
    }

    String contractData = ByteUtilities.toHexString(rawTx.getData().getDecodedContents());
    Map<String, List<String>> contractDataParams =
        config.getContractInterface().getContractParameters().parseTransfer(contractData);

    return contractDataParams.get(ContractParametersInterface.SENDER);
  }

  @Override
  public String signTransaction(String transaction, String address) {
    return signTransaction(transaction, address, null);
  }

  @Override
  public String signTransaction(String transaction, String address, String key) {
    return signTransaction(transaction, address, key, null);
  }

  @Override
  public String signTransaction(String transaction, String address, String key, String options) {
    return signTransaction(transaction, address, key, null, config);
  }

  public static String signTransaction(String transaction, String address, String key,
      String options, Configuration config) {
    // Convert transaction to data, and to parsed input.
    RawTransaction tx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    if (tx == null) {
      return transaction;
    }
    Iterable<Iterable<String>> sigData;

    if (key == null) {
      for (int i = 0; i < config.getMultiSigAccounts().length; i++) {
        if (config.getMultiSigAccounts()[i].isEmpty()) {
          continue;
        }
        sigData =
            Signatures.getSigString(transaction, config.getMultiSigAccounts()[i], false, config);
        sigData =
            Signatures.signWithPrivateKey(sigData, null, config.getMultiSigAccounts()[i], config);
        transaction = applySignature(transaction, address, sigData, config);
      }
      for (int i = 0; i < config.getMultiSigKeys().length; i++) {
        if (config.getMultiSigKeys()[i].isEmpty()) {
          continue;
        }
        String msigAddress = EthereumTools.getPublicAddress(config.getMultiSigKeys()[i], true);
        sigData = Signatures.getSigString(transaction, msigAddress, false, config);
        sigData = Signatures.signWithPrivateKey(sigData, config.getMultiSigKeys()[i], null, config);
        transaction = applySignature(transaction, address, sigData, config);
      }
    }

    sigData = Signatures.getSigString(transaction, address, false, config);
    sigData = Signatures.signWithPrivateKey(sigData, key, address, config);

    return applySignature(transaction, address, sigData, config);
  }

  @Override
  public Iterable<Iterable<String>> getSigString(String transaction, String address) {
    return Signatures.getSigString(transaction, address, false, config);
  }

  public static String applySignature(String transaction, String address,
      Iterable<Iterable<String>> signatureData, Configuration config) {
    // This is taken care of in the signing process for Ethereum, so we can just return the data.
    try {
      return signatureData.iterator().next().iterator().next();
    } catch (Exception e) {
      return "";
    }
  }

  @Override
  public String applySignature(String transaction, String address,
      Iterable<Iterable<String>> signatureData) {
    return applySignature(transaction, address, signatureData, config);
  }

  public static String sendTransaction(String transaction, Configuration config) {
    LOGGER.debug("Asked to send: " + transaction);
    RawTransaction rawTx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));

    if (rawTx == null) {
      return "Bad Transaction";
    }

    LOGGER.debug("Checking if we should re-sign with contract address...");
    LOGGER.debug("[TX Recipient] " + ByteUtilities.toHexString(rawTx.getTo().getDecodedContents()));
    LOGGER.debug("[StorageContract] " + config.getStorageContractAddress());

    if (ByteUtilities.toHexString(rawTx.getTo().getDecodedContents())
        .equalsIgnoreCase(config.getStorageContractAddress())) {
      LOGGER.debug("Recipient matches, finding function...");
      Map<String, List<String>> contractParams =
          config.getContractInterface().getContractParameters()
              .parseTransfer(ByteUtilities.toHexString(rawTx.getData().getDecodedContents()));
      Map<String, List<String>> adminParams = config.getContractInterface().getContractParameters()
          .parseAdminFunction(ByteUtilities.toHexString(rawTx.getData().getDecodedContents()));
      if (contractParams != null || adminParams != null) {
        LOGGER.debug("Found signable function.");
        String contractKey = config.getContractKey();
        String contractAddress = config.getContractAccount();

        if (!contractKey.isEmpty()) {
          contractAddress = EthereumTools.getPublicAddress(contractKey, true);
        } else {
          contractKey = null;
        }
        Iterable<Iterable<String>> sigData =
            Signatures.getSigString(transaction, contractAddress, true, config);
        sigData = Signatures
            .signWithPrivateKey(sigData, contractKey, contractKey == null ? contractAddress : null,
                config);
        LOGGER.debug("Re-signing offlineTransfer transaction");
        transaction = applySignature(transaction, contractAddress, sigData, config);
      }
    }

    LOGGER.debug("Sending: " + transaction);
    if (config.areTransactionsEnabled()) {
      return ethereumRpc.eth_sendRawTransaction("0x" + transaction);
    } else {
      return "Transactions Temporarily Disabled";
    }
  }

  @Override
  public String sendTransaction(String transaction) {
    return sendTransaction(transaction, config);
  }

  @Override
  public Map<String, String> getConfiguration() {
    HashMap<String, String> configSummary = new HashMap<>();
    configSummary.put("Currency Symbol", config.getCurrencySymbol());
    configSummary.put("Geth Connection", new EthereumConfiguration().getDaemonConnectionString());
    configSummary.put("Minimum Signatures", ((Integer) config.getMinSignatures()).toString());
    configSummary.put("Minimum Confirmations", ((Integer) config.getMinConfirmations()).toString());
    configSummary
        .put("Maximum Transaction Value", config.getMaxAmountPerTransaction().toPlainString());
    configSummary
        .put("Maximum Transaction Value Per Hour", config.getMaxAmountPerHour().toPlainString());
    configSummary
        .put("Maximum Transaction Value Per Day", config.getMaxAmountPerDay().toPlainString());
    configSummary.put("Contract", config.getStorageContractAddress());
    if (config.getContractKey() != null && !config.getContractKey().isEmpty()) {
      configSummary
          .put("Contract Manager", EthereumTools.getPublicAddress(config.getContractKey(), true));
    } else {
      configSummary.put("Contract Manager", config.getContractAccount());
    }
    return configSummary;
  }

  @Override
  public void enableTransactions() {
    config.setTransactionsEnabled(true);
  }

  @Override
  public void disableTransactions() {
    config.setTransactionsEnabled(false);
  }

  @Override
  public boolean transactionsEnabled() {
    return config.areTransactionsEnabled();
  }

  @Override
  public long getBlockchainHeight() {
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumRpc.eth_blockNumber()));
    return latestBlockNumber.longValue();
  }

  @Override
  public long getLastBlockTime() {
    Block block = ethereumRpc.eth_getBlockByNumber(DefaultBlock.LATEST.getValue(), true);
    BigInteger dateConverter = new BigInteger(1, ByteUtilities.toByteArray(block.getTimestamp()));
    return dateConverter.longValue();
  }

  private class TxDateComparator implements Comparator<TransactionDetails> {
    @Override
    public int compare(TransactionDetails o1, TransactionDetails o2) {
      return o1.getTxDate().compareTo(o2.getTxDate());
    }
  }

  @Override
  public TransactionDetails[] getTransactions(String address, int numberToReturn, int skipNumber) {
    LinkedList<TransactionDetails> txDetails = new LinkedList<>();

    Arrays.asList(Filters.getReconciliations(address, config)).forEach(txDetails::add);
    Arrays.asList(Filters.getTransfers(address, config)).forEach(txDetails::add);

    LOGGER.debug(Json.stringifyObject(LinkedList.class, txDetails));
    Collections.sort(txDetails, new TxDateComparator());
    for (int i = 0; i < skipNumber; i++) {
      txDetails.removeLast();
    }
    while (txDetails.size() > numberToReturn) {
      txDetails.removeFirst();
    }
    return txDetails.toArray(new TransactionDetails[txDetails.size()]);
  }

  @Override
  public TransactionDetails getTransaction(String transactionId) {
    Map txMap = ethereumRpc.eth_getTransactionByHash(transactionId);

    Block txBlock = ethereumRpc.eth_getBlockByNumber(txMap.get("blockNumber").toString(), true);
    TransactionDetails txDetail = new TransactionDetails();
    txDetail.setTxHash(txMap.get("hash").toString());
    txDetail.setTxDate(new Date(
        new BigInteger(1, ByteUtilities.toByteArray(txBlock.getTimestamp())).longValue() * 1000L));
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumRpc.eth_blockNumber()));
    BigInteger txBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(txMap.get("blockNumber").toString()));
    txDetail.setConfirmed(
        config.getMinConfirmations() <= latestBlockNumber.subtract(txBlockNumber).intValue());
    txDetail.setConfirmations(latestBlockNumber.subtract(txBlockNumber).intValue());
    txDetail.setMinConfirmations(config.getMinConfirmations());

    txDetail.setToAddress(new String[]{txMap.get("to").toString()});
    txDetail.setFromAddress(new String[]{txMap.get("from").toString()});

    LinkedList<TransactionDetails> txDetails = new LinkedList<>();
    Arrays.asList(Filters.getTransfers(null, config)).forEach(tx -> {
      if (tx.getTxHash().equalsIgnoreCase(transactionId)) {
        txDetails.add(tx);
      }
    });
    Arrays.asList(Filters.getReconciliations(null, config)).forEach(tx -> {
      if (tx.getTxHash().equalsIgnoreCase(transactionId)) {
        txDetails.add(tx);
      }
    });

    txDetail.setData(Json.stringifyObject(LinkedList.class, txDetails));
    if (txDetails.size() == 0) {
      return null;
    }
    return txDetail;
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
    return Signatures.signWithPrivateKey(data, privateKey, null, config);
  }
}
