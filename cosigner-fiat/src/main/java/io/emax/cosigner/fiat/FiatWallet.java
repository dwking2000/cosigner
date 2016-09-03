package io.emax.cosigner.fiat;

import io.emax.cosigner.api.core.ServerStatus;
import io.emax.cosigner.api.currency.CurrencyAdmin;
import io.emax.cosigner.api.currency.OfflineWallet;
import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.common.crypto.Secp256k1;
import io.emax.cosigner.ethereum.EthereumConfiguration;
import io.emax.cosigner.ethereum.EthereumResource;
import io.emax.cosigner.ethereum.common.EthereumTools;
import io.emax.cosigner.ethereum.common.RlpItem;
import io.emax.cosigner.ethereum.common.RlpList;
import io.emax.cosigner.ethereum.gethrpc.Block;
import io.emax.cosigner.ethereum.gethrpc.CallData;
import io.emax.cosigner.ethereum.gethrpc.DefaultBlock;
import io.emax.cosigner.ethereum.gethrpc.EthereumRpc;
import io.emax.cosigner.ethereum.gethrpc.RawTransaction;
import io.emax.cosigner.fiat.gethrpc.fiatcontract.FiatContract;
import io.emax.cosigner.fiat.gethrpc.fiatcontract.FiatContractInterface;
import io.emax.cosigner.fiat.gethrpc.fiatcontract.FiatContractParametersInterface;

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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FiatWallet implements Wallet, OfflineWallet, CurrencyAdmin {
  private static final Logger LOGGER = LoggerFactory.getLogger(FiatWallet.class);

  private static final String TESTNET_VERSION = "2";
  private static final long TESTNET_BASE_ROUNDS = (long) Math.pow(2, 20);

  // RPC and configuration
  private final EthereumRpc ethereumRpc = EthereumResource.getResource().getGethRpc();
  FiatConfiguration config;

  private String contractAddress = "";
  private FiatContractInterface contractInterface = new FiatContract();
  private HashSet<String> knownAddresses = new HashSet<>();
  private HashMap<String, HashSet<String>> ownedAddresses = new HashMap<>();

  // Transaction history data
  private final HashMap<String, HashSet<TransactionDetails>> txHistory = new HashMap<>();

  private Thread txFullHistorySubscription = new Thread(() -> {
    while (true) {
      try {
        LOGGER.info("Scanning FIAT:" + config.getCurrencySymbol() + " transactions");
        scanTransactions(0);
        Thread.sleep(60000);
      } catch (Exception e) {
        LOGGER.debug("Full transaction scan interrupted.");
      }
    }
  });

  private Thread txShortHistorySubscription = new Thread(() -> {
    while (true) {
      try {
        LOGGER.info("Short scanning FIAT:" + config.getCurrencySymbol() + " transactions");
        scanTransactions(-500);
        Thread.sleep(60000);
      } catch (Exception e) {
        LOGGER.debug("Short transaction scan interrupted.");
      }
    }
  });

  public FiatWallet(FiatConfiguration conf) {
    config = conf;
    setupFiatContract();

    if (!txFullHistorySubscription.isAlive()) {
      txFullHistorySubscription.setDaemon(true);
      txFullHistorySubscription.start();
    }

    if (!txShortHistorySubscription.isAlive()) {
      txShortHistorySubscription.setDaemon(true);
      txShortHistorySubscription.start();
    }
  }

  private void setupFiatContract() {
    LOGGER.info("[" + config.getCurrencySymbol() + "] Attempting to setup fiat contract");
    if (config.getContractAddress() != null && !config.getContractAddress().isEmpty()) {
      LOGGER.info("[" + config.getCurrencySymbol() + "] Using " + config.getContractAddress());
      contractAddress = config.getContractAddress();
      contractInterface = getContractType(contractAddress);
    } else {

      String contractKey = config.getContractKey();
      String contractAccount = config.getContractAccount();

      if (!contractKey.isEmpty()) {
        contractAccount = EthereumTools.getPublicAddress(contractKey, true);
        LOGGER.debug(
            "[" + config.getCurrencySymbol() + "] ContractAccount from key: " + contractAddress);
      }

      try {
        String txCount = ethereumRpc
            .eth_getTransactionCount("0x" + contractAccount, DefaultBlock.LATEST.toString());
        int rounds = new BigInteger(1, ByteUtilities.toByteArray(txCount)).intValue();
        int baseRounds = 0;
        if (ethereumRpc.net_version().equals(TESTNET_VERSION)) {
          baseRounds = (int) TESTNET_BASE_ROUNDS;
        }

        LOGGER.info(
            "[" + config.getCurrencySymbol() + "] FIAT Rounds: " + (rounds - baseRounds) + "("
                + txCount + " - " + baseRounds + ") for " + contractAccount);
        for (int i = baseRounds; i < rounds; i++) {
          if (i % 10000 == 0) {
            LOGGER.info(
                "[" + config.getCurrencySymbol() + "] FIAT Round progress: " + i + "/" + rounds
                    + "...");
          }
          RlpList contractAddress = new RlpList();
          RlpItem contractCreator = new RlpItem(ByteUtilities.toByteArray(contractAccount));
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

    try {
      if ((contractAddress == null || contractAddress.isEmpty()) && config.generateNewContract()) {
        LOGGER.info("[" + config.getCurrencySymbol() + "] Generating new contract...");
        // Create the TX data structure
        RawTransaction tx = new RawTransaction();
        tx.getGasPrice().setDecodedContents(ByteUtilities
            .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
        tx.getGasLimit().setDecodedContents(ByteUtilities
            .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));
        LinkedList<String> decodedAddresses = new LinkedList<>();
        decodedAddresses.addAll(Arrays.asList(config.getMultiSigAccounts()));
        Arrays.asList(config.getMultiSigKeys()).forEach(key -> {
          String address = EthereumTools.getPublicAddress(key, true);
          decodedAddresses.add(address);
        });
        tx.getData().setDecodedContents(ByteUtilities.toByteArray(
            contractInterface.getContractParameters()
                .createContract(config.getAdminAccount(), decodedAddresses,
                    config.getMinSignatures())));

        String rawTx = ByteUtilities.toHexString(tx.encode());
        LOGGER.debug("[" + config.getCurrencySymbol() + "] Creating contract: " + rawTx);
        String contractKey = config.getContractKey();
        contractAddress = config.getContractAccount();

        if (!contractKey.isEmpty()) {
          contractAddress = EthereumTools.getPublicAddress(contractKey, true);
          LOGGER.debug(
              "[" + config.getCurrencySymbol() + "] ContractAccount from key: " + contractAddress);
        } else {
          contractKey = null;
          LOGGER.debug("[" + config.getCurrencySymbol() + "] ContractAccount from config: "
              + contractAddress);
        }
        Iterable<Iterable<String>> sigData = getSigString(rawTx, contractAddress, true);
        sigData =
            signWithPrivateKey(sigData, contractKey, contractKey == null ? contractAddress : null);
        rawTx = applySignature(rawTx, contractAddress, sigData);
        LOGGER.debug("[" + config.getCurrencySymbol() + "] Signed contract: " + rawTx);

        sendTransaction(rawTx);

        RlpList calculatedContractAddress = new RlpList();
        RlpItem contractCreator = new RlpItem(ByteUtilities.toByteArray(contractAddress));
        calculatedContractAddress.add(contractCreator);
        calculatedContractAddress.add(tx.getNonce());

        // Figure out the contract address and store it in lookup tables for future use
        String expectedContract =
            EthereumTools.hashKeccak(ByteUtilities.toHexString(calculatedContractAddress.encode()))
                .substring(96 / 4, 256 / 4);

        LOGGER.debug("[" + config.getCurrencySymbol() + "] Expecting new contract address of "
            + expectedContract + " with tx: " + RawTransaction
            .parseBytes(ByteUtilities.toByteArray(rawTx)));
        contractAddress = expectedContract;
      }

      LOGGER
          .info("[" + config.getCurrencySymbol() + "] Got contract address of: " + contractAddress);
    } catch (Exception e) {
      LOGGER.error("[" + config.getCurrencySymbol()
          + "] Unable to create contract, FIAT module is not usable!");
      LOGGER.debug("[" + config.getCurrencySymbol() + "] Contract setup", e);
    }
  }

  private FiatContractInterface getContractType(String contract) {
    try {
      String contractCode = ethereumRpc
          .eth_getCode("0x" + contract.toLowerCase(Locale.US), DefaultBlock.LATEST.toString());
      contractCode = contractCode.substring(2);
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
      while (balanceCheck != null && balanceCheck != BigInteger.ZERO.toString(10)) {
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
    CallData callData = new CallData();
    callData.setTo("0x" + contractAddress);
    callData.setFrom("0x" + contractAddress);
    callData.setValue("0");
    callData.setData("0x" + contractInterface.getContractParameters().getBalance(address));
    callData.setGas("100000"); // Doesn't matter, just can't be nil
    callData.setGasPrice("100000"); // Doesn't matter, just can't be nil
    LOGGER.debug("Balance request: " + Json.stringifyObject(CallData.class, callData));
    String response = ethereumRpc.eth_call(callData, DefaultBlock.LATEST.toString());

    BigInteger balance = new BigInteger(1, ByteUtilities.toByteArray(response));
    return balance.toString(10);
  }

  public String getConfirmations(String address) {
    CallData callData = new CallData();
    callData.setTo("0x" + contractAddress);
    callData.setFrom("0x" + contractAddress);
    callData.setValue("0");
    callData.setData("0x" + contractInterface.getContractParameters().getConfirmations(address));
    callData.setGas("100000"); // Doesn't matter, just can't be nil
    callData.setGasPrice("100000"); // Doesn't matter, just can't be nil
    LOGGER.debug("Balance request: " + Json.stringifyObject(CallData.class, callData));
    String response = ethereumRpc.eth_call(callData, DefaultBlock.LATEST.toString());

    BigInteger balance = new BigInteger(1, ByteUtilities.toByteArray(response));
    return balance.toString(10);
  }

  public String getTotalBalances() {
    CallData callData = new CallData();
    callData.setTo("0x" + contractAddress);
    callData.setFrom("0x" + contractAddress);
    callData.setValue("0");
    callData.setData("0x" + contractInterface.getContractParameters().getTotalBalance());
    callData.setGas("100000"); // Doesn't matter, just can't be nil
    callData.setGasPrice("100000"); // Doesn't matter, just can't be nil
    LOGGER.debug("Total balance request: " + Json.stringifyObject(CallData.class, callData));
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

    String txCount = ethereumRpc
        .eth_getStorageAt("0x" + contractAddress.toLowerCase(Locale.US), "0x1",
            DefaultBlock.LATEST.toString());
    BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount)).add(BigInteger.ONE);

    tx.getData().setDecodedContents(ByteUtilities.toByteArray(
        contractInterface.getContractParameters()
            .transfer(nonce.longValue(), fromAddresses.iterator().next(), recipients, amounts,
                new LinkedList<>(), new LinkedList<>(), new LinkedList<>())));

    return ByteUtilities.toHexString(tx.encode());
  }

  @Override
  public Iterable<String> getSignersForTransaction(String transaction) {
    RawTransaction rawTx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    String contractData = ByteUtilities.toHexString(rawTx.getData().getDecodedContents());
    Map<String, List<String>> contractDataParams =
        contractInterface.getContractParameters().parseTransfer(contractData);

    return contractDataParams.get(FiatContractParametersInterface.SENDER);
  }

  @Override
  public String signTransaction(String transaction, String address) {
    return signTransaction(transaction, address, null);
  }

  @Override
  public String signTransaction(String transaction, String address, String name) {
    // Convert transaction to data, and to parsed input.
    RawTransaction tx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    Map<String, List<String>> txParams = contractInterface.getContractParameters()
        .parseTransfer(ByteUtilities.toHexString(tx.getData().getDecodedContents()));

    // If it's one of ours and the parameters are legible, verify it.
    if (txParams != null) {
      // Get the balance
      String balance =
          getBalance(txParams.get(contractInterface.getContractParameters().SENDER).get(0));
      String confirmations =
          getConfirmations(txParams.get(contractInterface.getContractParameters().SENDER).get(0));

      int intConfirmations = new BigInteger(confirmations, 10).intValue();
      int intBalance = new BigInteger(balance, 10).intValue();

      // Consider the balance invalid if there haven't been enough confirmations since the last tx.
      if (intConfirmations < config.getMinConfirmations()) {
        intBalance = 0;
      }

      int totalSent = 0;
      for (String amountString : txParams.get(contractInterface.getContractParameters().AMOUNT)) {
        totalSent += new BigInteger(amountString, 10).intValue();
      }

      // If it's not valid, return the original tx.
      if (totalSent > intBalance) {
        return transaction;
      }
    } // Otherwise just sign it.

    // Prepare sigData so that if we can't sign, it returns the original.
    LinkedList<String> txData = new LinkedList<>();
    txData.add(transaction);
    LinkedList<Iterable<String>> wrappedTxdata = new LinkedList<>();
    wrappedTxdata.add(txData);
    Iterable<Iterable<String>> sigData = wrappedTxdata;

    if (name == null && ownedAddresses.containsKey(address.toLowerCase(Locale.US))) {
      for (int i = 0; i < config.getMultiSigAccounts().length; i++) {
        if (config.getMultiSigAccounts()[i].isEmpty()) {
          continue;
        }
        sigData = getSigString(transaction, config.getMultiSigAccounts()[i]);
        sigData = signWithPrivateKey(sigData, null, config.getMultiSigAccounts()[i]);
        transaction = applySignature(transaction, address, sigData);
      }
      for (int i = 0; i < config.getMultiSigKeys().length; i++) {
        if (config.getMultiSigKeys()[i].isEmpty()) {
          continue;
        }
        String msigAddress = EthereumTools.getPublicAddress(config.getMultiSigKeys()[i], true);
        sigData = getSigString(transaction, msigAddress);
        sigData = signWithPrivateKey(sigData, config.getMultiSigKeys()[i], null);
        transaction = applySignature(transaction, address, sigData);
      }
    } else if (name == null) {
      sigData = getSigString(transaction, address);
      sigData = signWithPrivateKey(sigData, address, null);
      transaction = applySignature(transaction, address, sigData);
    } else {
      String translatedAddress = address.toLowerCase(Locale.US);
      sigData = getSigString(transaction, translatedAddress);
      sigData = signWithPrivateKey(sigData, name, translatedAddress);
    }

    return applySignature(transaction, address, sigData);
  }

  @Override
  public Iterable<Iterable<String>> getSigString(String transaction, String address) {
    return getSigString(transaction, address, false);
  }

  public Iterable<Iterable<String>> getSigString(String transaction, String address,
      boolean ignoreContractCode) {
    RawTransaction tx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    LinkedList<Iterable<String>> sigStrings = new LinkedList<>();

    if (!ignoreContractCode && ByteUtilities.toHexString(tx.getTo().getDecodedContents())
        .equalsIgnoreCase(contractAddress)) {
      // Initialize hash (IV = 0x00)
      String hashBytes = String.format("%64s", "0").replace(' ', '0');

      // Get the transaction data
      Map<String, List<String>> contractParams = contractInterface.getContractParameters()
          .parseTransfer(ByteUtilities.toHexString(tx.getData().getDecodedContents()));
      if (contractParams != null) {
        LOGGER.debug(Json.stringifyObject(Map.class, contractParams));

        BigInteger nonce =
            new BigInteger(contractParams.get(FiatContractParametersInterface.NONCE).get(0));
        List<String> recipients = contractParams.get(FiatContractParametersInterface.RECIPIENTS);
        List<String> amounts = contractParams.get(FiatContractParametersInterface.AMOUNT);

        // Hash to sign is hash(previous hash + recipient + amount + nonce)
        for (int i = 0; i < recipients.size(); i++) {
          hashBytes += String.format("%40s", recipients.get(i)).replace(' ', '0');
          hashBytes += String.format("%64s",
              ByteUtilities.toHexString(new BigInteger(amounts.get(i)).toByteArray()))
              .replace(' ', '0');
          hashBytes += String.format("%64s", ByteUtilities.toHexString(nonce.toByteArray()))
              .replace(' ', '0');

          LOGGER.debug("Hashing: " + hashBytes);
          hashBytes = EthereumTools.hashKeccak(hashBytes);
          LOGGER.debug("Result: " + hashBytes);
        }
        LinkedList<String> msigString = new LinkedList<>();
        msigString.add(contractInterface.getClass().getCanonicalName());
        msigString.add(hashBytes);
        sigStrings.add(msigString);
      }
    }

    // Calculate the transaction's signature data.
    String txCount =
        ethereumRpc.eth_getTransactionCount("0x" + address, DefaultBlock.LATEST.toString());
    LinkedList<String> txString = new LinkedList<>();
    txString.add(transaction);
    txString.add(txCount);
    sigStrings.add(txString);

    LOGGER.debug(sigStrings.toString());

    return sigStrings;
  }

  @Override
  public String applySignature(String transaction, String address,
      Iterable<Iterable<String>> signatureData) {
    // This is taken care of in the signing process for Ethereum, so we can just return the data.
    try {
      return signatureData.iterator().next().iterator().next();
    } catch (Exception e) {
      return "";
    }
  }

  @Override
  public String sendTransaction(String transaction) {
    LOGGER.debug("Asked to send: " + transaction);
    RawTransaction rawTx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));

    if (ByteUtilities.toHexString(rawTx.getTo().getDecodedContents())
        .equalsIgnoreCase(contractAddress)) {
      Map<String, List<String>> contractParams = contractInterface.getContractParameters()
          .parseTransfer(ByteUtilities.toHexString(rawTx.getData().getDecodedContents()));
      if (contractParams != null) {
        String contractKey = config.getContractKey();
        String contractAddress = config.getContractAccount();

        if (!contractKey.isEmpty()) {
          contractAddress = EthereumTools.getPublicAddress(contractKey, true);
        } else {
          contractKey = null;
        }
        Iterable<Iterable<String>> sigData = getSigString(transaction, contractAddress, true);
        sigData =
            signWithPrivateKey(sigData, contractKey, contractKey == null ? contractAddress : null);
        LOGGER.debug("Re-signing transfer transaction");
        transaction = applySignature(transaction, contractAddress, sigData);
      }
    }

    LOGGER.debug("Sending: " + transaction);
    if (transactionsEnabled) {
      return ethereumRpc.eth_sendRawTransaction(transaction);
    } else {
      return "Transactions Temporarily Disabled";
    }
  }

  private void scanTransactions(long startingBlock) {
    // Scan every block, look for origin and receiver.
    try {
      // Get latest block
      BigInteger latestBlockNumber =
          new BigInteger(1, ByteUtilities.toByteArray(ethereumRpc.eth_blockNumber()));

      // If it's negative, start from that many blocks prior to the last.
      if (startingBlock < 0) {
        startingBlock = Math.max(startingBlock + latestBlockNumber.longValue(), 0);
      }

      for (long i = startingBlock; i < latestBlockNumber.longValue(); i++) {
        String blockNumber = "0x" + BigInteger.valueOf(i).toString(16);
        Block block;
        try {
          block = ethereumRpc.eth_getBlockByNumber(blockNumber, true);
        } catch (Exception e) {
          LOGGER.warn("Error reading block #" + i, e);
          continue;
        }

        if (block.getTransactions().length == 0) {
          continue;
        }

        Arrays.asList(block.getTransactions()).forEach(tx -> {
          TransactionDetails txDetail = new TransactionDetails();
          BigInteger dateConverter =
              new BigInteger(1, ByteUtilities.toByteArray(block.getTimestamp()));
          dateConverter = dateConverter.multiply(BigInteger.valueOf(1000));
          txDetail.setTxDate(new Date(dateConverter.longValue()));

          txDetail.setTxHash(ByteUtilities.toHexString(ByteUtilities.toByteArray(tx.getHash())));
          txDetail.setFromAddress(
              new String[]{ByteUtilities.toHexString(ByteUtilities.toByteArray(tx.getFrom()))});
          txDetail.setToAddress(
              new String[]{ByteUtilities.toHexString(ByteUtilities.toByteArray(tx.getTo()))});
          txDetail.setAmount(BigDecimal.ZERO);

          BigInteger txBlockNumber = new BigInteger(1, ByteUtilities.toByteArray(blockNumber));
          txDetail.setConfirmed(
              config.getMinConfirmations() <= latestBlockNumber.subtract(txBlockNumber).intValue());
          txDetail.setConfirmations(latestBlockNumber.subtract(txBlockNumber).intValue());
          txDetail.setMinConfirmations(config.getMinConfirmations());

          // For each receiver that is the fiat account, parse the data, check if it's transferring a balance
          try {
            if (this.contractAddress
                .equalsIgnoreCase(txDetail.getToAddress()[0].toLowerCase(Locale.US))) {
              String txData = tx.getInput();
              FiatContractParametersInterface contractParamsInterface =
                  this.contractInterface.getContractParameters();
              Map<String, List<String>> contractParams =
                  contractParamsInterface.parseTransfer(txData);

              if (contractParams != null) {
                for (int j = 0; j < contractParams.get(contractParamsInterface.RECIPIENTS).size();
                     j++) {
                  TransactionDetails fiatTx = new TransactionDetails();
                  fiatTx.setTxDate(new Date(dateConverter.longValue()));
                  fiatTx.setFromAddress(
                      contractParams.get(contractParamsInterface.SENDER).toArray(new String[]{}));
                  fiatTx.setToAddress(
                      new String[]{contractParams.get(contractParamsInterface.RECIPIENTS).get(j)});
                  fiatTx.setAmount(
                      new BigDecimal(contractParams.get(contractParamsInterface.AMOUNT).get(j)));
                  fiatTx.setTxHash(txDetail.getTxHash());
                  fiatTx.setConfirmed(txDetail.isConfirmed());
                  fiatTx.setConfirmations(latestBlockNumber.subtract(txBlockNumber).intValue());
                  fiatTx.setMinConfirmations(config.getMinConfirmations());

                  if (!txHistory.containsKey(fiatTx.getToAddress()[0])) {
                    txHistory.put(fiatTx.getToAddress()[0], new HashSet<>());
                  }
                  if (!txHistory.containsKey(fiatTx.getFromAddress()[0])) {
                    txHistory.put(fiatTx.getFromAddress()[0], new HashSet<>());
                  }

                  txHistory.get(fiatTx.getFromAddress()[0]).add(fiatTx);
                  txHistory.get(fiatTx.getToAddress()[0]).add(fiatTx);
                }
              }
            }
          } catch (Exception e) {
            LOGGER.debug("Unable to decode tx data");
          }
        });
      }
    } catch (Exception e) {
      LOGGER.warn("Unable to scan blockchain for FIAT wallet!");
    }
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
    configSummary.put("Contract", this.contractAddress);
    if (config.getContractKey() != null && !config.getContractKey().isEmpty()) {
      configSummary
          .put("Contract Manager", EthereumTools.getPublicAddress(config.getContractKey(), true));
    } else {
      configSummary.put("Contract Manager", config.getContractAccount());
    }
    return configSummary;
  }

  private boolean transactionsEnabled = true;

  @Override
  public void enableTransactions() {
    transactionsEnabled = true;
  }

  @Override
  public void disableTransactions() {
    transactionsEnabled = false;
  }

  @Override
  public boolean transactionsEnabled() {
    return transactionsEnabled;
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
    if (txHistory.containsKey(address)) {
      txHistory.get(address).forEach(txDetails::add);
    }

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

    try {
      if (contractAddress.equalsIgnoreCase(txDetail.getToAddress()[0].toLowerCase(Locale.US))) {
        String txData = txMap.get("input").toString();
        FiatContractParametersInterface contractParamsInterface =
            contractInterface.getContractParameters();
        Map<String, List<String>> contractParams = contractParamsInterface.parseTransfer(txData);

        if (contractParams != null) {
          for (int j = 0; j < contractParams.get(contractParamsInterface.RECIPIENTS).size(); j++) {
            txDetail.setFromAddress(
                contractParams.get(contractParamsInterface.SENDER).toArray(new String[]{}));
            txDetail.setToAddress(
                new String[]{contractParams.get(contractParamsInterface.RECIPIENTS).get(j)});
            txDetail.setAmount(
                new BigDecimal(contractParams.get(contractParamsInterface.AMOUNT).get(j)));
          }
        }
      }
    } catch (Exception e) {
      LOGGER.debug("Unable to decode tx data");
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
    return signWithPrivateKey(data, privateKey, null);
  }

  private Iterable<Iterable<String>> signWithPrivateKey(Iterable<Iterable<String>> data,
      String privateKey, String address) {
    LOGGER.debug("Attempting to sign: " + address + data.toString());

    LinkedList<Iterable<String>> signedData = new LinkedList<>();
    LinkedList<Iterable<String>> listedData = new LinkedList<>();
    data.forEach(listedData::add);
    LinkedList<String> contractData = new LinkedList<>();
    LinkedList<String> txData = new LinkedList<>();
    // Check if there are two entries, if there are, the first one should be mSig data.
    int txDataLocation = 0;
    if (listedData.size() == 2) {
      txDataLocation++;
      listedData.get(0).forEach(contractData::add);
    }
    listedData.get(txDataLocation).forEach(txData::add);

    try {
      // Sign mSig if there is any
      if (contractData.size() > 0) {
        LOGGER.debug("Reading mSig data");
        String sigBytes = contractData.getLast();
        byte[][] sigData = signData(sigBytes, address, privateKey);
        // Return the original TX on failure
        if (sigData.length < 3) {
          LinkedList<String> signature = new LinkedList<>();
          signature.add(txData.getFirst());
          LinkedList<Iterable<String>> result = new LinkedList<>();
          result.add(signature);
          return result;
        }

        LinkedList<String> msigSig = new LinkedList<>();
        msigSig.add(ByteUtilities.toHexString(sigData[0]));
        msigSig.add(ByteUtilities.toHexString(sigData[1]));
        msigSig.add(ByteUtilities.toHexString(sigData[2]));
        signedData.add(msigSig);
      } else {
        LOGGER.debug("No mSig data to process.");
      }
      // Rebuild the TX if there is any mSig data
      RawTransaction rawTx =
          RawTransaction.parseBytes(ByteUtilities.toByteArray(txData.getFirst()));
      // If we've added mSig data then update the TX.
      if (signedData.size() > 0) {
        String contractVersion = contractData.getFirst();
        FiatContractInterface contract =
            (FiatContractInterface) FiatContractInterface.class.getClassLoader()
                .loadClass(contractVersion).newInstance();

        FiatContractParametersInterface contractParms = contract.getContractParameters();
        Map<String, List<String>> contractParamData = contractParms
            .parseTransfer(ByteUtilities.toHexString(rawTx.getData().getDecodedContents()));

        Iterator<String> msigSig = signedData.getFirst().iterator();
        contractParamData.get(FiatContractParametersInterface.SIGR).add(msigSig.next());
        contractParamData.get(FiatContractParametersInterface.SIGS).add(msigSig.next());
        contractParamData.get(FiatContractParametersInterface.SIGV).add(msigSig.next());

        Long nonce =
            new BigInteger(contractParamData.get(FiatContractParametersInterface.NONCE).get(0))
                .longValue();
        String sender = contractParamData.get(FiatContractParametersInterface.SENDER).get(0);
        List<String> recipients = contractParamData.get(FiatContractParametersInterface.RECIPIENTS);
        List<Long> amounts = new LinkedList<>();
        for (String amount : contractParamData.get(FiatContractParametersInterface.AMOUNT)) {
          amounts.add(new BigInteger(amount).longValue());
        }
        List<String> sigV = contractParamData.get(FiatContractParametersInterface.SIGV);
        List<String> sigR = contractParamData.get(FiatContractParametersInterface.SIGR);
        List<String> sigS = contractParamData.get(FiatContractParametersInterface.SIGS);

        rawTx.getData().setDecodedContents(ByteUtilities.toByteArray(
            contractParms.transfer(nonce, sender, recipients, amounts, sigV, sigR, sigS)));
      }
      // Sign the TX
      String txCount = txData.getLast();
      BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount));

      if (nonce.equals(BigInteger.ZERO)) {
        rawTx.getNonce().setDecodedContents(new byte[]{});
      } else {
        rawTx.getNonce()
            .setDecodedContents(ByteUtilities.stripLeadingNullBytes(nonce.toByteArray()));
      }

      String sigString = ByteUtilities.toHexString(rawTx.getSigBytes());
      LOGGER.debug("Tx: " + ByteUtilities.toHexString(rawTx.encode()));
      LOGGER.debug("SigBytes: " + sigString);
      sigString = EthereumTools.hashKeccak(sigString);
      LOGGER.debug("Hashed: " + sigString);
      byte[][] sigData = signData(sigString, address, privateKey);
      if (sigData.length < 3) {
        LinkedList<String> signature = new LinkedList<>();
        signature.add(txData.getFirst());
        LinkedList<Iterable<String>> result = new LinkedList<>();
        result.add(signature);
        return result;
      }

      rawTx.getSigR().setDecodedContents(sigData[0]);
      rawTx.getSigS().setDecodedContents(sigData[1]);
      rawTx.getSigV().setDecodedContents(sigData[2]);

      // Return the signed TX as-is, we don't need network information to apply it.
      LinkedList<String> signature = new LinkedList<>();
      signature.add(ByteUtilities.toHexString(rawTx.encode()));
      LinkedList<Iterable<String>> result = new LinkedList<>();
      result.add(signature);
      result.add(new LinkedList<>(Collections.singletonList(sigString)));
      return result;
    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
      LOGGER.warn(null, e);
      LinkedList<String> signature = new LinkedList<>();
      signature.add(txData.getFirst());
      LinkedList<Iterable<String>> result = new LinkedList<>();
      result.add(signature);
      return result;
    }
  }

  private byte[][] signData(String data, String address, String name) {
    if (name == null) {
      // Catch errors here
      String sig;
      try {
        LOGGER.debug("Asking geth to sign " + data + " for 0x" + address);
        sig = ethereumRpc.eth_sign("0x" + address, data);
      } catch (Exception e) {
        LOGGER.warn(null, e);
        return new byte[0][0];
      }

      try {
        LOGGER.debug("Decoding sig result: " + sig);
        byte[] sigBytes = ByteUtilities.toByteArray(sig);
        byte[] sigR = Arrays.copyOfRange(sigBytes, 0, 32);
        byte[] sigS = Arrays.copyOfRange(sigBytes, 32, 64);
        byte[] sigV = Arrays.copyOfRange(sigBytes, 64, 65);

        String signingAddress = null;
        try {
          signingAddress = ByteUtilities.toHexString(
              Secp256k1.recoverPublicKey(sigR, sigS, sigV, ByteUtilities.toByteArray(data)))
              .substring(2);
        } catch (Exception e) {
          LOGGER.debug("Couldn't recover public key from signature", e);
        }
        signingAddress = EthereumTools.getPublicAddress(signingAddress, false);
        LOGGER.debug("Appears to be signed by: " + signingAddress);

        // Adjust for expected format.
        sigV[0] += 27;

        return new byte[][]{sigR, sigS, sigV};
      } catch (Exception e) {
        LOGGER.error(null, e);
        return new byte[0][0];
      }
    } else {
      int maxRounds = 1000;

      String privateKey = "";
      if (address != null) {
        for (int i = 1; i <= maxRounds; i++) {
          String privateKeyCheck =
              EthereumTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), i);
          if (EthereumTools.getPublicAddress(privateKeyCheck).equalsIgnoreCase(address)) {
            privateKey = privateKeyCheck;
            break;
          }
        }
        if (privateKey.isEmpty()) {
          return new byte[0][0];
        }
      } else {
        privateKey = name;
        address = EthereumTools.getPublicAddress(privateKey);
      }

      // Sign and return it
      byte[] privateBytes = ByteUtilities.toByteArray(privateKey);
      byte[] sigBytes = ByteUtilities.toByteArray(data);
      String signingAddress = "";

      // The odd signature can't be resolved to a recoveryId, in those cases, just sign it again.
      byte[] sigV;
      byte[] sigR;
      byte[] sigS;
      do {
        byte[][] signedBytes = Secp256k1.signTransaction(sigBytes, privateBytes);
        // EIP-2
        BigInteger lowSlimit =
            new BigInteger("007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF5D576E7357A4501DDFE92F46681B20A0",
                16);
        BigInteger ourSvalue = new BigInteger(1, signedBytes[1]);
        while (ourSvalue.compareTo(lowSlimit) > 0) {
          signedBytes = Secp256k1.signTransaction(sigBytes, privateBytes);
          ourSvalue = new BigInteger(1, signedBytes[1]);
        }
        sigR = ByteUtilities.stripLeadingNullBytes(signedBytes[0]);
        sigS = ByteUtilities.stripLeadingNullBytes(signedBytes[1]);
        sigV = signedBytes[2];

        if (sigV[0] != 0 && sigV[0] != 1) {
          continue;
        }

        try {
          signingAddress =
              ByteUtilities.toHexString(Secp256k1.recoverPublicKey(sigR, sigS, sigV, sigBytes))
                  .substring(2);
        } catch (Exception e) {
          LOGGER.debug("Couldn't recover the public key", e);
        }
        signingAddress = EthereumTools.getPublicAddress(signingAddress, false);
      } while (!address.equalsIgnoreCase(signingAddress));

      // Adjust for ethereum's encoding
      sigV[0] += 27;

      return new byte[][]{sigR, sigS, sigV};
    }
  }

  public String generateTokens(long amount) {
    RawTransaction tx = new RawTransaction();
    tx.getTo().setDecodedContents(ByteUtilities.toByteArray(contractAddress));
    tx.getGasPrice().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
    tx.getGasLimit().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));
    tx.getData().setDecodedContents(
        ByteUtilities.toByteArray(contractInterface.getContractParameters().createTokens(amount)));

    return ByteUtilities.toHexString(tx.encode());
  }

  public String destroyTokens(long amount) {
    RawTransaction tx = new RawTransaction();
    tx.getTo().setDecodedContents(ByteUtilities.toByteArray(contractAddress));
    tx.getGasPrice().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
    tx.getGasLimit().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));
    tx.getData().setDecodedContents(
        ByteUtilities.toByteArray(contractInterface.getContractParameters().destroyTokens(amount)));

    return ByteUtilities.toHexString(tx.encode());
  }
}
