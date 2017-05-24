package io.emax.cosigner.ethereum.token;

import io.emax.cosigner.api.core.ServerStatus;
import io.emax.cosigner.api.currency.CurrencyAdmin;
import io.emax.cosigner.api.currency.OfflineWallet;
import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.common.crypto.Secp256k1;
import io.emax.cosigner.ethereum.core.EthereumConfiguration;
import io.emax.cosigner.ethereum.core.EthereumResource;
import io.emax.cosigner.ethereum.core.common.EthereumTools;
import io.emax.cosigner.ethereum.core.gethrpc.Block;
import io.emax.cosigner.ethereum.core.gethrpc.CallData;
import io.emax.cosigner.ethereum.core.gethrpc.DefaultBlock;
import io.emax.cosigner.ethereum.core.gethrpc.EthereumRpc;
import io.emax.cosigner.ethereum.core.gethrpc.RawTransaction;
import io.emax.cosigner.ethereum.token.gethrpc.tokencontract.TokenContract;
import io.emax.cosigner.ethereum.token.gethrpc.tokencontract.TokenContractInterface;
import io.emax.cosigner.ethereum.token.gethrpc.tokencontract.TokenContractParametersInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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
import java.util.Random;
import java.util.Set;

public class TokenWallet implements Wallet, OfflineWallet, CurrencyAdmin {
  private static final Logger LOGGER = LoggerFactory.getLogger(TokenWallet.class);

  private static final String TESTNET_VERSION = "2";
  private static final long TESTNET_BASE_ROUNDS = (long) Math.pow(2, 20);

  // RPC and configuration
  private final EthereumRpc ethereumRpc = EthereumResource.getResource().getGethRpc();
  TokenConfiguration config;

  private String storageContractAddress = "";
  private String tokenContractAddress = "";
  private String adminContractAddress = "";
  private TokenContractInterface contractInterface = new TokenContract();
  private HashSet<String> knownAddresses = new HashSet<>();
  private HashMap<String, HashSet<String>> ownedAddresses = new HashMap<>();

  public TokenWallet(TokenConfiguration conf) {
    config = conf;
    setupTokenContract();
  }

  private void findExistingContract(String contractAccount) {
    try {
      String txCount = ethereumRpc
          .eth_getTransactionCount("0x" + contractAccount, DefaultBlock.LATEST.toString());
      int rounds = new BigInteger(1, ByteUtilities.toByteArray(txCount)).intValue();
      int baseRounds = 0;
      if (ethereumRpc.net_version().equals(TESTNET_VERSION)) {
        baseRounds = (int) TESTNET_BASE_ROUNDS;
      }

      LOGGER.info(
          "[" + config.getCurrencySymbol() + "] Token Rounds: " + (rounds - baseRounds) + "("
              + txCount + " - " + baseRounds + ") for " + contractAccount);
      for (int i = baseRounds; i < rounds; i++) {
        if (i % 10000 == 0) {
          LOGGER.info(
              "[" + config.getCurrencySymbol() + "] Token Round progress: " + i + "/" + rounds
                  + "...");
        }

        String contract = EthereumTools.calculateContractAddress(contractAccount, (long) i);
        TokenContractInterface contractClass = getContractVersion(contract);

        if (storageContractAddress.isEmpty() && contractClass != null) {
          this.contractInterface = contractClass;
        }

        String contractType = getContractType(contract);
        if (adminContractAddress.isEmpty() && contractType != null && contractType
            .equalsIgnoreCase(ADMIN)) {
          this.adminContractAddress = contract;
        } else if (tokenContractAddress.isEmpty() && contractType != null && contractType
            .equalsIgnoreCase(TOKEN)) {
          this.tokenContractAddress = contract;
        } else if (storageContractAddress.isEmpty() && contractType != null && contractType
            .equalsIgnoreCase(STORAGE)) {
          this.storageContractAddress = contract;
        } else if (!adminContractAddress.isEmpty() && !tokenContractAddress.isEmpty()
            && !storageContractAddress.isEmpty()) {
          break;
        }
      }
    } catch (Exception e) {
      LOGGER.debug(null, e);
    }
  }

  private String waitForReceipt(String txId) {
    String minedContractAddress = null;
    int confirmations = 0;
    Map<String, Object> receiptData = ethereumRpc.eth_getTransactionReceipt(txId);
    try {
      while (receiptData == null || config.getMinConfirmations() > confirmations) {
        LOGGER.info("Waiting for transaction receipt...");
        Thread.sleep(5000);
        receiptData = ethereumRpc.eth_getTransactionReceipt(txId);
        if (receiptData != null) {
          minedContractAddress = (String) receiptData.get("contractAddress");
          minedContractAddress =
              ByteUtilities.toHexString(ByteUtilities.toByteArray(minedContractAddress));
          BigInteger latestBlockNumber =
              new BigInteger(1, ByteUtilities.toByteArray(ethereumRpc.eth_blockNumber()));
          BigInteger txBlockNumber =
              new BigInteger(1, ByteUtilities.toByteArray((String) receiptData.get("blockNumber")));
          confirmations = latestBlockNumber.subtract(txBlockNumber).intValue();

          LOGGER.info("[TX Receipt] Got " + config.getCurrencySymbol() + " contract address: "
              + minedContractAddress);
          LOGGER.info("[TX Receipt] Confirmations: " + confirmations);
        }
      }
      return minedContractAddress;
    } catch (Exception e) {
      LOGGER.debug("Interrupted while waiting for tx receipt", e);
      return null;
    }
  }

  public void setupTokenContract() {
    LOGGER.info("[" + config.getCurrencySymbol() + "] Attempting to setup token contract");
    if (config.getStorageContractAddress() != null && !config.getStorageContractAddress()
        .isEmpty()) {
      // Contract info specified in configuration, using that.
      LOGGER
          .info("[" + config.getCurrencySymbol() + "] Using " + config.getStorageContractAddress());
      storageContractAddress = config.getStorageContractAddress();
      tokenContractAddress = config.getTokenContractAddress();
      adminContractAddress = config.getAdminContractAddress();
      contractInterface = getContractVersion(storageContractAddress);
      if (contractInterface == null) {
        contractInterface = new TokenContract();
      }
    } else {
      // Attempting to find existing contract on the blockchain given our configuration.
      String contractKey = config.getContractKey();
      String contractAccount = config.getContractAccount();

      if (!contractKey.isEmpty()) {
        contractAccount = EthereumTools.getPublicAddress(contractKey, true);
        LOGGER.debug(
            "[" + config.getCurrencySymbol() + "] ContractAccount from key: " + contractAccount);
      } else {
        contractKey = null;
        LOGGER.debug(
            "[" + config.getCurrencySymbol() + "] ContractAccount from config: " + contractAccount);
      }

      findExistingContract(contractAccount);

      try {
        if ((storageContractAddress == null || storageContractAddress.isEmpty()) && config
            .generateNewContract()) {
          // Could not find an existing contract, attempting to register a new instance of it.
          LOGGER.info("[" + config.getCurrencySymbol() + "] Generating new contract...");

          if (config.generateTokenContract()) {
            // Admin contract
            // Gather owner addresses
            LinkedList<String> decodedAddresses = new LinkedList<>();
            decodedAddresses.addAll(Arrays.asList(config.getMultiSigAccounts()));
            Arrays.asList(config.getMultiSigKeys()).forEach(key -> {
              if (key.isEmpty()) {
                return;
              }
              String address = EthereumTools.getPublicAddress(key, true);
              decodedAddresses.add(address);
            });
            decodedAddresses.removeIf(String::isEmpty);

            // Generating tx structure
            RawTransaction tx = RawTransaction.createContract(config,
                contractInterface.getContractParameters()
                    .createAdminContract(config.getAdminAccount(), decodedAddresses,
                        config.getMinSignatures()));
            String rawTx = ByteUtilities.toHexString(tx.encode());
            LOGGER.debug("[" + config.getCurrencySymbol() + "] Creating contract: " + rawTx);

            // Signing it
            Iterable<Iterable<String>> sigData = getSigString(rawTx, contractAccount, true);
            sigData = signWithPrivateKey(sigData, contractKey,
                contractKey == null ? contractAccount : null);
            rawTx = applySignature(rawTx, contractAccount, sigData);
            LOGGER.debug("[" + config.getCurrencySymbol() + "] Signed contract: " + rawTx);

            // Wait for receipt
            String txId = sendTransaction(rawTx);
            adminContractAddress = waitForReceipt(txId);

            // Token Contract
            // Generate tx structure
            tx = RawTransaction.createContract(config, contractInterface.getContractParameters()
                .createTokenContract(this.adminContractAddress, config.getCurrencySymbol(),
                    config.getCurrencySymbol(), (int) config.getDecimalPlaces()));
            rawTx = ByteUtilities.toHexString(tx.encode());
            LOGGER.debug("[" + config.getCurrencySymbol() + "] Creating contract: " + rawTx);

            // Sign it
            sigData = getSigString(rawTx, contractAccount, true);
            sigData = signWithPrivateKey(sigData, contractKey,
                contractKey == null ? contractAccount : null);
            rawTx = applySignature(rawTx, contractAccount, sigData);
            LOGGER.debug("[" + config.getCurrencySymbol() + "] Signed contract: " + rawTx);

            // Wait for receipt
            txId = sendTransaction(rawTx);
            tokenContractAddress = waitForReceipt(txId);

            // Token Contract Assignment
            // Generate tx structure
            Long nonce = contractInterface.getContractParameters()
                .getNonce(ethereumRpc, this.adminContractAddress);
            tx = RawTransaction.createTransaction(config, adminContractAddress, null,
                contractInterface.getContractParameters()
                    .setTokenChild(nonce, tokenContractAddress, new LinkedList<>(),
                        new LinkedList<>(), new LinkedList<>()));
            rawTx = ByteUtilities.toHexString(tx.encode());
            LOGGER.debug("[" + config.getCurrencySymbol() + "] Issuing transaction: " + rawTx);

            // Sign it
            // Admin key first, because it has to be there.
            sigData = getSigString(rawTx, contractAccount, false);
            sigData = signWithPrivateKey(sigData, config.getAdminKey(),
                config.getAdminKey().isEmpty() ? config.getAdminContractAddress() : null);
            rawTx = applySignature(rawTx, contractAccount, sigData);

            // Sign with any multi-sig keys configured to meet minimum req's
            rawTx = signTransaction(rawTx, contractAccount);

            // Sign with contract account because it's the one that should have funds in it
            sigData = getSigString(rawTx, contractAccount, false);
            sigData = signWithPrivateKey(sigData, contractKey, null);
            rawTx = applySignature(rawTx, contractAccount, sigData);

            LOGGER.debug("[" + config.getCurrencySymbol() + "] Signed transaction: " + rawTx);

            // Wait for receipt
            txId = sendTransaction(rawTx);
            waitForReceipt(txId);
          }

          // Work around for invalid token configuration when running on the ether version
          if (config.useAlternateEtherContract()) {
            tokenContractAddress = "0x12345678901234567890";
          }

          if (!tokenContractAddress.isEmpty()) {
            LinkedList<String> decodedAddresses = new LinkedList<>();
            decodedAddresses.addAll(Arrays.asList(config.getMultiSigAccounts()));
            Arrays.asList(config.getMultiSigKeys()).forEach(key -> {
              if (key.isEmpty()) {
                return;
              }
              String address = EthereumTools.getPublicAddress(key, true);
              decodedAddresses.add(address);
            });
            decodedAddresses.removeIf(String::isEmpty);

            // Generating tx structure
            RawTransaction tx = RawTransaction.createContract(config,
                contractInterface.getContractParameters()
                    .createStorageContract(config, tokenContractAddress, config.getAdminAccount(),
                        decodedAddresses, config.getMinSignatures(), new Random().nextLong(),
                        config.getCurrencySymbol(), config.getCurrencySymbol(),
                        (int) config.getDecimalPlaces()));
            String rawTx = ByteUtilities.toHexString(tx.encode());
            LOGGER.debug("[" + config.getCurrencySymbol() + "] Creating contract: " + rawTx);

            // Signing it
            Iterable<Iterable<String>> sigData = getSigString(rawTx, contractAccount, true);
            sigData = signWithPrivateKey(sigData, contractKey,
                contractKey == null ? contractAccount : null);
            rawTx = applySignature(rawTx, contractAccount, sigData);
            LOGGER.debug("[" + config.getCurrencySymbol() + "] Signed contract: " + rawTx);

            // Wait for receipt
            String txId = sendTransaction(rawTx);
            storageContractAddress = waitForReceipt(txId);
          } else {
            throw new Exception(
                "Could not create storage contract, no token contract address available!");
          }
        }
        LOGGER.info("[" + config.getCurrencySymbol() + "] Got contract address of: "
            + storageContractAddress);
      } catch (Exception e) {
        LOGGER.error("[" + config.getCurrencySymbol()
            + "] Unable to create contract, Token module is not usable!");
        LOGGER.debug("[" + config.getCurrencySymbol() + "] Contract setup", e);
      }
      LOGGER.debug("[" + config.getCurrencySymbol() + "] Admin Contract: " + adminContractAddress);
      LOGGER.debug("[" + config.getCurrencySymbol() + "] Token Contract: " + tokenContractAddress);
      LOGGER.debug(
          "[" + config.getCurrencySymbol() + "] Storage Contract: " + storageContractAddress);
    }
  }

  public TokenContractInterface getContractVersion(String contract) {
    try {
      String contractCode = ethereumRpc
          .eth_getCode("0x" + contract.toLowerCase(Locale.US), DefaultBlock.LATEST.toString());
      contractCode = contractCode.substring(2);
      Class<?> contractType = TokenContract.class;
      while (TokenContractInterface.class.isAssignableFrom(contractType)) {
        TokenContractInterface contractParams = (TokenContractInterface) contractType.newInstance();
        if (contractParams.getStorageRuntime().equalsIgnoreCase(contractCode) || contractParams
            .getAdminRuntime().equalsIgnoreCase(contractCode)) {
          return contractParams;
        } else if (config.useAlternateEtherContract() && contractParams.getAlternateStorageRunTime()
            .equalsIgnoreCase(contractCode)) {
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

  private static final String ADMIN = "admin";
  private static final String TOKEN = "token";
  private static final String STORAGE = "storage";

  public String getContractType(String contract) {
    try {
      String contractCode = ethereumRpc
          .eth_getCode("0x" + contract.toLowerCase(Locale.US), DefaultBlock.LATEST.toString());
      contractCode = contractCode.substring(2);
      Class<?> contractType = TokenContract.class;
      while (TokenContractInterface.class.isAssignableFrom(contractType)) {
        TokenContractInterface contractParams = (TokenContractInterface) contractType.newInstance();
        if (contractParams.getAdminRuntime().equalsIgnoreCase(contractCode)) {
          return ADMIN;
        } else if (contractParams.getTokenRuntime().equalsIgnoreCase(contractCode)) {
          return TOKEN;
        } else if (contractParams.getStorageRuntime().equalsIgnoreCase(contractCode)) {
          return STORAGE;
        } else if (config.useAlternateEtherContract() && contractParams.getAlternateStorageRunTime()
            .equalsIgnoreCase(contractCode)) {
          return STORAGE;
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
        .generateCall(contractInterface.getContractParameters().getBalance(address),
            storageContractAddress);
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
        .generateCall(contractInterface.getContractParameters().getTotalBalance(),
            storageContractAddress);
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
    String contract = storageContractAddress;
    TokenContractInterface txInterface = getContractVersion(firstSender);
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

      RawTransaction tx = RawTransaction.createTransaction(config, tokenContractAddress, null,
          contractInterface.getContractParameters().tokenTransfer(rcpt, amount));

      return ByteUtilities.toHexString(tx.encode());
    } else {
      if (txInterface == null) {
        txInterface = contractInterface;
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
              .transfer(nonce.longValue(), firstSender, recipients, amounts, new LinkedList<>(),
                  new LinkedList<>(), new LinkedList<>()));

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
        contractInterface.getContractParameters().parseTransfer(contractData);

    return contractDataParams.get(TokenContractParametersInterface.SENDER);
  }

  @Override
  public String signTransaction(String transaction, String address) {
    return signTransaction(transaction, address, null);
  }

  @Override
  public String signTransaction(String transaction, String address, String name) {
    return signTransaction(transaction, address, name, null);
  }

  @Override
  public String signTransaction(String transaction, String address, String name, String options) {
    // Convert transaction to data, and to parsed input.
    RawTransaction tx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    if (tx == null) {
      return transaction;
    }
    Iterable<Iterable<String>> sigData;

    if (name == null) {
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
    }

    sigData = getSigString(transaction, address);
    sigData = signWithPrivateKey(sigData, name, address);

    return applySignature(transaction, address, sigData);
  }

  @Override
  public Iterable<Iterable<String>> getSigString(String transaction, String address) {
    return getSigString(transaction, address, false);
  }

  private Iterable<Iterable<String>> getSigString(String transaction, String address,
      boolean ignoreContractCode) {
    RawTransaction tx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    LinkedList<Iterable<String>> sigStrings = new LinkedList<>();

    if (tx == null) {
      LOGGER.warn("Not able to parse tx.");
      LinkedList<String> txData = new LinkedList<>();
      txData.add(transaction);
      LinkedList<Iterable<String>> wrappedTxData = new LinkedList<>();
      wrappedTxData.add(txData);
      return wrappedTxData;
    }

    if (!ignoreContractCode) {
      LOGGER.debug("Attempting to parse contract code...");
      String hashBytes;

      // Get the transaction data
      TokenContractInterface txContractInterface =
          getContractVersion(ByteUtilities.toHexString(tx.getTo().getDecodedContents()));

      LOGGER.debug("Found contract interface: " + (txContractInterface == null ? "null" :
          txContractInterface.getClass().getCanonicalName()) + " at " + ByteUtilities
          .toHexString(tx.getTo().getDecodedContents()));

      if (txContractInterface != null) {
        Map<String, List<String>> contractParams = txContractInterface.getContractParameters()
            .parseTransfer(ByteUtilities.toHexString(tx.getData().getDecodedContents()));
        if (contractParams != null) {
          LOGGER.debug(Json.stringifyObject(Map.class, contractParams));

          BigInteger nonce =
              new BigInteger(contractParams.get(TokenContractParametersInterface.NONCE).get(0));
          List<String> recipients = contractParams.get(TokenContractParametersInterface.RECIPIENTS);
          List<String> amounts = contractParams.get(TokenContractParametersInterface.AMOUNT);

          // Hash to sign is hash(previous hash + recipient + amount + nonce)
          hashBytes = txContractInterface.getContractParameters()
              .calculateTxHash(nonce.longValue(), recipients, amounts);
          LinkedList<String> msigString = new LinkedList<>();
          msigString.add(txContractInterface.getClass().getCanonicalName());
          msigString.add(hashBytes);
          sigStrings.add(msigString);
        } else {
          contractParams = txContractInterface.getContractParameters()
              .parseAdminFunction(ByteUtilities.toHexString(tx.getData().getDecodedContents()));
          if (contractParams != null) {
            // Sign it as an admin function
            BigInteger nonce =
                new BigInteger(contractParams.get(TokenContractParametersInterface.NONCE).get(0));

            hashBytes = txContractInterface.getContractParameters().calculateAdminHash(ethereumRpc,
                ByteUtilities.toHexString(tx.getTo().getDecodedContents()), nonce.longValue());
            LOGGER.debug("Result: " + hashBytes);
            LinkedList<String> msigString = new LinkedList<>();
            msigString.add(txContractInterface.getClass().getCanonicalName());
            msigString.add(hashBytes);
            sigStrings.add(msigString);
          }
        }
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

    if (rawTx == null) {
      return "Bad Transaction";
    }

    if (ByteUtilities.toHexString(rawTx.getTo().getDecodedContents())
        .equalsIgnoreCase(storageContractAddress)) {
      Map<String, List<String>> contractParams = contractInterface.getContractParameters()
          .parseTransfer(ByteUtilities.toHexString(rawTx.getData().getDecodedContents()));
      Map<String, List<String>> adminParams = contractInterface.getContractParameters()
          .parseAdminFunction(ByteUtilities.toHexString(rawTx.getData().getDecodedContents()));
      if (contractParams != null || adminParams != null) {
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
      return ethereumRpc.eth_sendRawTransaction("0x" + transaction);
    } else {
      return "Transactions Temporarily Disabled";
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
    configSummary.put("Contract", this.storageContractAddress);
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

  @Override
  public long getBlockchainHeight() {
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumRpc.eth_blockNumber()));
    return latestBlockNumber.longValue();
  }

  @Override
  public long getLastBlockTime() {
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumRpc.eth_blockNumber()));
    Block block = ethereumRpc.eth_getBlockByNumber(latestBlockNumber.toString(), true);
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

    Arrays.asList(getReconciliations(address)).forEach(txDetails::add);
    Arrays.asList(getTransfers(address)).forEach(txDetails::add);

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

  private TransactionDetails[] getTransfers(String address) {
    // Get latest block
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumRpc.eth_blockNumber()));

    LinkedList<TransactionDetails> txDetails = new LinkedList<>();
    Map<String, Object> filterParams = new HashMap<>();
    filterParams.put("fromBlock", "0x00");
    filterParams.put("toBlock", "latest");
    filterParams.put("address", "0x" + storageContractAddress);
    LinkedList<String> functionTopics = new LinkedList<>();
    functionTopics.add("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef");
    functionTopics.add("0x5548c837ab068cf56a2c2479df0882a4922fd203edb7517321831d95078c5f62");
    for (String functionTopic : functionTopics) {
      Object[] topicArray = new Object[1];
      String[] senderTopic = {functionTopic};
      topicArray[0] = senderTopic;
      filterParams.put("topics", topicArray);
      LOGGER.debug("Requesting filter for: " + Json.stringifyObject(Map.class, filterParams));
      String txFilter = ethereumRpc.eth_newFilter(filterParams);
      LOGGER.debug("Setup filter: " + txFilter);
      Map<String, Object>[] filterResults;
      try {
        LOGGER.debug("Getting filter results...");
        filterResults = ethereumRpc.eth_getFilterLogs(txFilter);
      } catch (Exception e) {
        LOGGER.debug("Something went wrong", e);
        filterResults = new Map[0];
      } finally {
        ethereumRpc.eth_uninstallFilter(txFilter);
      }
      for (Map<String, Object> result : filterResults) {
        LOGGER.debug(result.toString());
        TransactionDetails txDetail = new TransactionDetails();
        txDetail.setTxHash((String) result.get("transactionHash"));
        try {
          Block block = ethereumRpc.eth_getBlockByNumber((String) result.get("blockNumber"), true);
          BigInteger dateConverter =
              new BigInteger(1, ByteUtilities.toByteArray(block.getTimestamp()));
          dateConverter = dateConverter.multiply(BigInteger.valueOf(1000));
          txDetail.setTxDate(new Date(dateConverter.longValue()));

          BigInteger txBlockNumber =
              new BigInteger(1, ByteUtilities.toByteArray((String) result.get("blockNumber")));
          txDetail.setConfirmed(
              config.getMinConfirmations() <= latestBlockNumber.subtract(txBlockNumber).intValue());
          txDetail.setConfirmations(latestBlockNumber.subtract(txBlockNumber).intValue());
          txDetail.setMinConfirmations(config.getMinConfirmations());

          ArrayList<String> topics = (ArrayList<String>) result.get("topics");

          if (!topics.get(0).equalsIgnoreCase(functionTopic)) {
            continue;
          }

          String from = ByteUtilities.toHexString(
              ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(1))));
          txDetail.setFromAddress(new String[]{from});

          String to = ByteUtilities.toHexString(
              ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(2))));
          txDetail.setToAddress(new String[]{to});

          String amount = ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(
              ByteUtilities
                  .readBytes(ByteUtilities.toByteArray((String) result.get("data")), 0, 32)));
          txDetail.setAmount(new BigDecimal(new BigInteger(1, ByteUtilities.toByteArray(amount)))
              .setScale(20, BigDecimal.ROUND_UNNECESSARY)
              .divide(BigDecimal.valueOf(10).pow((int) config.getDecimalPlaces()),
                  BigDecimal.ROUND_UNNECESSARY));

          if (address == null || ByteUtilities.toHexString(
              ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(1))))
              .equalsIgnoreCase(address) || ByteUtilities.toHexString(
              ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(2))))
              .equalsIgnoreCase(address)) {
            txDetails.add(txDetail);
          }
        } catch (Exception e) {
          // Pending TX
          LOGGER.debug("Pending Tx Found or wrong event returned by geth.");
          LOGGER.trace(null, e);
        }
      }
    }

    LOGGER.debug(Json.stringifyObject(LinkedList.class, txDetails));
    Collections.sort(txDetails, new TxDateComparator());
    return txDetails.toArray(new TransactionDetails[txDetails.size()]);
  }

  private TransactionDetails[] getReconciliations(String address) {
    // Get latest block
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumRpc.eth_blockNumber()));

    LinkedList<TransactionDetails> txDetails = new LinkedList<>();
    Map<String, Object> filterParams = new HashMap<>();
    filterParams.put("fromBlock", "0x00");
    filterParams.put("toBlock", "latest");
    filterParams.put("address", "0x" + storageContractAddress);
    LinkedList<String> functionTopics = new LinkedList<>();
    functionTopics.add("0x73bb00f3ad09ef6bc524e5cf56563dff2bc6663caa0b4054aa5946811083ed2e");
    for (String functionTopic : functionTopics) {
      Object[] topicArray = new Object[1];
      String[] senderTopic = {functionTopic};
      topicArray[0] = senderTopic;
      filterParams.put("topics", topicArray);
      LOGGER.debug(
          "Requesting reconciliation filter for: " + Json.stringifyObject(Map.class, filterParams));
      String txFilter = ethereumRpc.eth_newFilter(filterParams);
      LOGGER.debug("Setup filter: " + txFilter);
      Map<String, Object>[] filterResults;
      try {
        LOGGER.debug("Getting filter results...");
        filterResults = ethereumRpc.eth_getFilterLogs(txFilter);
      } catch (Exception e) {
        LOGGER.debug("Something went wrong", e);
        filterResults = new Map[0];
      }
      for (Map<String, Object> result : filterResults) {
        LOGGER.debug(result.toString());
        TransactionDetails txDetail = new TransactionDetails();
        txDetail.setTxHash((String) result.get("transactionHash"));
        try {
          Block block = ethereumRpc.eth_getBlockByNumber((String) result.get("blockNumber"), true);
          BigInteger dateConverter =
              new BigInteger(1, ByteUtilities.toByteArray(block.getTimestamp()));
          dateConverter = dateConverter.multiply(BigInteger.valueOf(1000));
          txDetail.setTxDate(new Date(dateConverter.longValue()));

          BigInteger txBlockNumber =
              new BigInteger(1, ByteUtilities.toByteArray((String) result.get("blockNumber")));
          txDetail.setConfirmed(
              config.getMinConfirmations() <= latestBlockNumber.subtract(txBlockNumber).intValue());
          txDetail.setConfirmations(latestBlockNumber.subtract(txBlockNumber).intValue());
          txDetail.setMinConfirmations(config.getMinConfirmations());

          ArrayList<String> topics = (ArrayList<String>) result.get("topics");

          if (!topics.get(0).equalsIgnoreCase(functionTopic)) {
            continue;
          }

          String amount = ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(
              ByteUtilities
                  .readBytes(ByteUtilities.toByteArray((String) result.get("data")), 0, 32)));
          txDetail.setAmount(new BigDecimal(new BigInteger(ByteUtilities.toByteArray(amount)))
              .setScale(20, BigDecimal.ROUND_UNNECESSARY)
              .divide(BigDecimal.valueOf(10).pow((int) config.getDecimalPlaces()),
                  BigDecimal.ROUND_UNNECESSARY));

          if (BigInteger.ZERO.compareTo(new BigInteger(ByteUtilities.toByteArray(amount))) > 0) {
            String from = ByteUtilities.toHexString(
                ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(1))));
            txDetail.setFromAddress(new String[]{from});
          } else {
            String to = ByteUtilities.toHexString(
                ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(1))));
            txDetail.setToAddress(new String[]{to});
          }

          if (address == null || ByteUtilities.toHexString(
              ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(1))))
              .equalsIgnoreCase(address)) {
            txDetails.add(txDetail);
          }

        } catch (Exception e) {
          // Pending TX
          LOGGER.debug("Pending Tx Found or wrong event returned by geth.");
          LOGGER.trace(null, e);
        }
      }
    }

    LOGGER.debug(Json.stringifyObject(LinkedList.class, txDetails));
    Collections.sort(txDetails, new TxDateComparator());
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
    Arrays.asList(getTransfers(null)).forEach(tx -> {
      if (tx.getTxHash().equalsIgnoreCase(transactionId)) {
        txDetails.add(tx);
      }
    });
    Arrays.asList(getReconciliations(null)).forEach(tx -> {
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

      if (rawTx == null) {
        LinkedList<String> signature = new LinkedList<>();
        signature.add(txData.getFirst());
        LinkedList<Iterable<String>> result = new LinkedList<>();
        result.add(signature);
        return result;
      }

      // If we've added mSig data then update the TX.
      // Get the contract that corresponds to the recipients code if possible.
      TokenContractInterface txContractInterface =
          getContractVersion(ByteUtilities.toHexString(rawTx.getTo().getDecodedContents()));
      if (txContractInterface == null) {
        txContractInterface = contractInterface;
      }
      if (signedData.size() > 0 && txContractInterface.getContractParameters()
          .parseTransfer(ByteUtilities.toHexString(rawTx.getData().getDecodedContents())) != null) {
        // There are new signatures and the TX appears to be a transfer
        // Load the right contract version so we put the data in the right places.
        String contractVersion = contractData.getFirst();
        TokenContractInterface contract =
            (TokenContractInterface) TokenContractInterface.class.getClassLoader()
                .loadClass(contractVersion).newInstance();
        TokenContractParametersInterface contractParms = contract.getContractParameters();
        Map<String, List<String>> contractParamData = contractParms
            .parseTransfer(ByteUtilities.toHexString(rawTx.getData().getDecodedContents()));

        // Append the signature data to data structures
        Iterator<String> msigSig = signedData.getFirst().iterator();
        contractParamData.get(TokenContractParametersInterface.SIGR).add(msigSig.next());
        contractParamData.get(TokenContractParametersInterface.SIGS).add(msigSig.next());
        contractParamData.get(TokenContractParametersInterface.SIGV).add(msigSig.next());

        // Convert all the components into values we can pass into the transfer call
        Long nonce =
            new BigInteger(contractParamData.get(TokenContractParametersInterface.NONCE).get(0))
                .longValue();
        String sender = contractParamData.get(TokenContractParametersInterface.SENDER).get(0);
        List<String> recipients =
            contractParamData.get(TokenContractParametersInterface.RECIPIENTS);
        List<BigInteger> amounts = new LinkedList<>();
        for (String amount : contractParamData.get(TokenContractParametersInterface.AMOUNT)) {
          amounts.add(new BigInteger(amount));
        }
        List<String> sigV = contractParamData.get(TokenContractParametersInterface.SIGV);
        List<String> sigR = contractParamData.get(TokenContractParametersInterface.SIGR);
        List<String> sigS = contractParamData.get(TokenContractParametersInterface.SIGS);

        // Rebuild the function data
        rawTx.getData().setDecodedContents(ByteUtilities.toByteArray(
            contractParms.transfer(nonce, sender, recipients, amounts, sigV, sigR, sigS)));
      } else if (signedData.size() > 0 && txContractInterface.getContractParameters()
          .parseAdminFunction(ByteUtilities.toHexString(rawTx.getData().getDecodedContents()))
          != null) {
        // There are new signatures and the TX appears to be an admin function
        // Get the right contract version.
        String contractVersion = contractData.getFirst();
        TokenContractInterface contract =
            (TokenContractInterface) TokenContractInterface.class.getClassLoader()
                .loadClass(contractVersion).newInstance();
        TokenContractParametersInterface contractParms = contract.getContractParameters();

        Map<String, List<String>> contractParamData = contractParms
            .parseAdminFunction(ByteUtilities.toHexString(rawTx.getData().getDecodedContents()));

        // Update the contract data with the new signatures.
        Iterator<String> msigSig = signedData.getFirst().iterator();
        contractParamData.get(TokenContractParametersInterface.SIGR).add(msigSig.next());
        contractParamData.get(TokenContractParametersInterface.SIGS).add(msigSig.next());
        contractParamData.get(TokenContractParametersInterface.SIGV).add(msigSig.next());

        // Rebuild the function data
        rawTx.getData().setDecodedContents(
            ByteUtilities.toByteArray(contractParms.rebuildAdminFunction(contractParamData)));
      }

      // Sign the TX itself so it can be broadcast.
      String txCount = txData.getLast();
      BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount));

      // RLP formatting quirks
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
        // Signature data is bad, return the original transaction.
        LinkedList<String> signature = new LinkedList<>();
        signature.add(txData.getFirst());
        LinkedList<Iterable<String>> result = new LinkedList<>();
        result.add(signature);
        return result;
      }

      // Apply the signature to the tx structure.
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
      // Something went very wrong, return the original transaction.
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
      // We have nothing to go on but address, we have to ask a 3rd party to sign for us.
      // At this point, that's geth/whatever node software we're using.
      String sig;
      try {
        LOGGER.debug("Asking geth to sign 0x" + data + " for 0x" + address);
        sig = ethereumRpc.eth_sign("0x" + address, "0x" + data);
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
          LOGGER.debug("Couldn't recover public key from signature");
          LOGGER.trace(null, e);
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
      int maxRounds = 100;

      // Attempt to recover the address as if it's a user key.
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
        // If we couldn't match the address, assume the name is actually a private key
        if (privateKey.isEmpty()) {
          privateKey = name;
          address = EthereumTools.getPublicAddress(privateKey);
        }
      } else {
        // If the address is empty assume the name is a private key and generate it from that.
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
          LOGGER.debug("Couldn't recover the public key");
          LOGGER.trace(null, e);
        }
        signingAddress = EthereumTools.getPublicAddress(signingAddress, false);
      } while (!address.equalsIgnoreCase(signingAddress));

      // Adjust for ethereum's encoding
      sigV[0] += 27;

      return new byte[][]{sigR, sigS, sigV};
    }
  }

  public String generateTokens(String recipient, long amount) {
    Long nonce =
        contractInterface.getContractParameters().getNonce(ethereumRpc, adminContractAddress);
    RawTransaction tx = RawTransaction.createTransaction(config, adminContractAddress, null,
        contractInterface.getContractParameters()
            .createTokens(nonce, recipient, amount, new LinkedList<>(), new LinkedList<>(),
                new LinkedList<>()));

    return ByteUtilities.toHexString(tx.encode());
  }

  public String destroyTokens(String sender, long amount) {
    Long nonce =
        contractInterface.getContractParameters().getNonce(ethereumRpc, adminContractAddress);
    RawTransaction tx = RawTransaction.createTransaction(config, adminContractAddress, null,
        contractInterface.getContractParameters()
            .destroyTokens(nonce, sender, amount, new LinkedList<>(), new LinkedList<>(),
                new LinkedList<>()));

    return ByteUtilities.toHexString(tx.encode());
  }

  public String reconcile(Map<String, BigInteger> addressChanges) {
    Long nonce =
        contractInterface.getContractParameters().getNonce(ethereumRpc, storageContractAddress);
    RawTransaction tx = RawTransaction.createTransaction(config, storageContractAddress, null,
        contractInterface.getContractParameters()
            .reconcile(nonce, addressChanges, new LinkedList<>(), new LinkedList<>(),
                new LinkedList<>()));

    return ByteUtilities.toHexString(tx.encode());
  }

  public String allowance(String owner, String grantee) {
    CallData callData = EthereumTools
        .generateCall(contractInterface.getContractParameters().allowance(owner, grantee),
            tokenContractAddress);
    LOGGER.debug("Balance request: " + Json.stringifyObject(CallData.class, callData));

    return ethereumRpc.eth_call(callData, DefaultBlock.LATEST.toString());
  }

  public String approve(String grantee, BigDecimal amount) {
    RawTransaction tx = RawTransaction.createTransaction(config, tokenContractAddress, null,
        contractInterface.getContractParameters().approve(grantee,
            amount.multiply(BigDecimal.TEN.pow((int) config.getDecimalPlaces())).toBigInteger()));

    return ByteUtilities.toHexString(tx.encode());
  }
}
