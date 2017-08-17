package io.emax.cosigner.ethereum.tokenstorage;

import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.ethereum.core.common.EthereumTools;
import io.emax.cosigner.ethereum.core.gethrpc.DefaultBlock;
import io.emax.cosigner.ethereum.core.gethrpc.RawTransaction;
import io.emax.cosigner.ethereum.tokenstorage.contract.Contract;
import io.emax.cosigner.ethereum.tokenstorage.contract.ContractInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

/**
 * Token contract creation logic.
 */
public class Setup {
  private static final Logger LOGGER = LoggerFactory.getLogger(Setup.class);

  /**
   * Search through the contract creator's transaction history for a previously existing account.
   *
   * Allows for easier recovery when these addresses are not committed to the configuration, but
   * should not be relied upon for production use.
   */
  static void findExistingContract(String contractAccount, Configuration config) {
    try {
      String txCount = Base.ethereumRpc
          .eth_getTransactionCount("0x" + contractAccount, DefaultBlock.LATEST.toString());
      int rounds = new BigInteger(1, ByteUtilities.toByteArray(txCount)).intValue();
      int baseRounds = 0;
      if (Base.ethereumRpc.net_version().equals(Base.TESTNET_VERSION)) {
        baseRounds = (int) Base.TESTNET_BASE_ROUNDS;
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
        ContractInterface contractClass = Utilities.getContractVersion(contract, config);

        if (config.getStorageContractAddress().isEmpty() && contractClass != null) {
          config.setContractInterface(contractClass);
        }

        String contractType = Utilities.getContractType(contract, config);
        if (config.getAdminContractAddress().isEmpty() && contractType != null && contractType
            .equalsIgnoreCase(Base.ADMIN)) {
          config.setAdminContractAddress(contract);
        } else if (config.getTokenContractAddress().isEmpty() && contractType != null
            && contractType.equalsIgnoreCase(Base.TOKEN)) {
          config.setTokenContractAddress(contract);
        } else if (config.getStorageContractAddress().isEmpty() && contractType != null
            && contractType.equalsIgnoreCase(Base.STORAGE)) {
          config.setStorageContractAddress(contract);
        } else if (!config.getAdminContractAddress().isEmpty() && !config.getTokenContractAddress()
            .isEmpty() && !config.getStorageContractAddress().isEmpty()) {
          break;
        }
      }
    } catch (Exception e) {
      LOGGER.debug(null, e);
    }
  }

  /**
   * Waits for a transaction receipt and returns the contract address if applicable.
   *
   * Useful for transactions that must be confirmed before continuing, such as needing to be sure of
   * a contract address.
   */
  static String waitForReceipt(String txId, Configuration config) {
    String minedContractAddress = null;
    int confirmations = 0;
    Map<String, Object> receiptData = Base.ethereumRpc.eth_getTransactionReceipt(txId);
    try {
      while (receiptData == null || config.getMinConfirmations() > confirmations) {
        LOGGER.info("Waiting for transaction receipt...");
        Thread.sleep(5000);
        receiptData = Base.ethereumRpc.eth_getTransactionReceipt(txId);
        if (receiptData != null) {
          minedContractAddress = (String) receiptData.get("contractAddress");
          minedContractAddress =
              ByteUtilities.toHexString(ByteUtilities.toByteArray(minedContractAddress));
          BigInteger latestBlockNumber =
              new BigInteger(1, ByteUtilities.toByteArray(Base.ethereumRpc.eth_blockNumber()));
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

  /**
   * Creates the contracts if configured to do so.
   */
  public static void setupTokenContract(Configuration config) {
    LOGGER.info("[" + config.getCurrencySymbol() + "] Attempting to setup tokenstorage contract");
    if (config.getStorageContractAddress() != null && !config.getStorageContractAddress()
        .isEmpty()) {
      // Contract info specified in configuration, using that.
      LOGGER
          .info("[" + config.getCurrencySymbol() + "] Using " + config.getStorageContractAddress());
      config.setContractInterface(
          Utilities.getContractVersion(config.getStorageContractAddress(), config));
      if (config.getContractInterface() == null) {
        config.setContractInterface(new Contract());
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

      findExistingContract(contractAccount, config);

      try {
        if ((config.getStorageContractAddress() == null || config.getStorageContractAddress()
            .isEmpty()) && config.generateNewContract()) {
          // Could not find an existing contract, attempting to register a new instance of it.
          LOGGER.info("[" + config.getCurrencySymbol() + "] Generating new contract...");

          if (config.generateTokenContract() && config.getContractInterface()
              .hasAdminAndTokenContracts()) {
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
                config.getContractInterface().getContractParameters()
                    .createAdminContract(config.getAdminAccount(), decodedAddresses,
                        config.getMinSignatures()));
            String rawTx = ByteUtilities.toHexString(tx.encode());
            LOGGER.debug("[" + config.getCurrencySymbol() + "] Creating contract: " + rawTx);

            // Signing it
            Iterable<Iterable<String>> sigData =
                Signatures.getSigString(rawTx, contractAccount, true, config);
            sigData = Signatures.signWithPrivateKey(sigData, contractKey,
                contractKey == null ? contractAccount : null, config);
            rawTx = Wallet.applySignature(rawTx, contractAccount, sigData, config);
            LOGGER.debug("[" + config.getCurrencySymbol() + "] Signed contract: " + rawTx);

            // Wait for receipt
            String txId = Wallet.sendTransaction(rawTx, config);
            config.setAdminContractAddress(waitForReceipt(txId, config));

            // Token Contract
            // Generate tx structure
            tx = RawTransaction.createContract(config,
                config.getContractInterface().getContractParameters()
                    .createTokenContract(config.getAdminContractAddress(),
                        config.getCurrencySymbol(), config.getCurrencySymbol(),
                        (int) config.getDecimalPlaces()));
            rawTx = ByteUtilities.toHexString(tx.encode());
            LOGGER.debug("[" + config.getCurrencySymbol() + "] Creating contract: " + rawTx);

            // Sign it
            sigData = Signatures.getSigString(rawTx, contractAccount, true, config);
            sigData = Signatures.signWithPrivateKey(sigData, contractKey,
                contractKey == null ? contractAccount : null, config);
            rawTx = Wallet.applySignature(rawTx, contractAccount, sigData, config);
            LOGGER.debug("[" + config.getCurrencySymbol() + "] Signed contract: " + rawTx);

            // Wait for receipt
            txId = Wallet.sendTransaction(rawTx, config);
            config.setTokenContractAddress(waitForReceipt(txId, config));

            // Token Contract Assignment
            // Generate tx structure
            Long nonce = config.getContractInterface().getContractParameters()
                .getNonce(Base.ethereumRpc, config.getAdminContractAddress(),
                    config.getAdminAccount());
            tx = RawTransaction.createTransaction(config, config.getAdminContractAddress(), null,
                config.getContractInterface().getContractParameters()
                    .setTokenChild(nonce, config.getTokenContractAddress(), new LinkedList<>(),
                        new LinkedList<>(), new LinkedList<>()));
            rawTx = ByteUtilities.toHexString(tx.encode());
            LOGGER.debug("[" + config.getCurrencySymbol() + "] Issuing transaction: " + rawTx);

            // Sign it
            // Admin key first, because it has to be there.
            sigData = Signatures.getSigString(rawTx, contractAccount, false, config);
            sigData = Signatures.signWithPrivateKey(sigData, config.getAdminKey(),
                config.getAdminKey().isEmpty() ? config.getAdminContractAddress() : null, config);
            rawTx = Wallet.applySignature(rawTx, contractAccount, sigData, config);

            // Sign with any multi-sig keys configured to meet minimum req's
            rawTx = Wallet.signTransaction(rawTx, contractAccount, null, null, config);

            // Sign with contract account because it's the one that should have funds in it
            sigData = Signatures.getSigString(rawTx, contractAccount, false, config);
            sigData = Signatures.signWithPrivateKey(sigData, contractKey, null, config);
            rawTx = Wallet.applySignature(rawTx, contractAccount, sigData, config);

            LOGGER.debug("[" + config.getCurrencySymbol() + "] Signed transaction: " + rawTx);

            // Wait for receipt
            txId = Wallet.sendTransaction(rawTx, config);
            waitForReceipt(txId, config);
          }

          // Work around for invalid tokenstorage configuration when running on the ether version
          if (config.useAlternateEtherContract()) {
            config.setTokenContractAddress("12345678901234567890");
          }

          if (!config.getTokenContractAddress().isEmpty()) {
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
                config.getContractInterface().getContractParameters()
                    .createStorageContract(config, config.getTokenContractAddress(),
                        config.getAdminAccount(), decodedAddresses, config.getMinSignatures(),
                        new Random().nextLong(), config.getCurrencySymbol(),
                        config.getCurrencySymbol(), (int) config.getDecimalPlaces()));
            String rawTx = ByteUtilities.toHexString(tx.encode());
            LOGGER.debug("[" + config.getCurrencySymbol() + "] Creating contract: " + rawTx);

            // Signing it
            Iterable<Iterable<String>> sigData =
                Signatures.getSigString(rawTx, contractAccount, true, config);
            sigData = Signatures.signWithPrivateKey(sigData, contractKey,
                contractKey == null ? contractAccount : null, config);
            rawTx = Wallet.applySignature(rawTx, contractAccount, sigData, config);
            LOGGER.debug("[" + config.getCurrencySymbol() + "] Signed contract: " + rawTx);

            // Wait for receipt
            String txId = Wallet.sendTransaction(rawTx, config);
            config.setStorageContractAddress(waitForReceipt(txId, config));
          } else {
            throw new Exception(
                "Could not create storage contract, no tokenstorage contract address available!");
          }
        }
        LOGGER.info("[" + config.getCurrencySymbol() + "] Got contract address of: " + config
            .getStorageContractAddress());
      } catch (Exception e) {
        LOGGER.error("[" + config.getCurrencySymbol()
            + "] Unable to create contract, Token module is not usable!");
        LOGGER.debug("[" + config.getCurrencySymbol() + "] Contract setup", e);
      }
      LOGGER.debug("[" + config.getCurrencySymbol() + "] Admin Contract: " + config
          .getAdminContractAddress());
      LOGGER.debug("[" + config.getCurrencySymbol() + "] Token Contract: " + config
          .getTokenContractAddress());
      LOGGER.debug("[" + config.getCurrencySymbol() + "] Storage Contract: " + config
          .getStorageContractAddress());
    }
  }
}
