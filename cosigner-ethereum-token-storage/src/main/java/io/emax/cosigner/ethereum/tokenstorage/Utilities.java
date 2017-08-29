package io.emax.cosigner.ethereum.tokenstorage;

import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.ethereum.core.common.EthereumTools;
import io.emax.cosigner.ethereum.core.gethrpc.CallData;
import io.emax.cosigner.ethereum.core.gethrpc.DefaultBlock;
import io.emax.cosigner.ethereum.core.gethrpc.RawTransaction;
import io.emax.cosigner.ethereum.tokenstorage.contract.Contract;
import io.emax.cosigner.ethereum.tokenstorage.contract.ContractInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

import static io.emax.cosigner.ethereum.tokenstorage.Base.ethereumRpc;

/**
 * Common utility functions for Tokens.
 */
public class Utilities {
  private static final Logger LOGGER = LoggerFactory.getLogger(Utilities.class);

  /**
   * Determines if the contract at the provided address matches the byte code for
   * ADMIN/TOKEN/STORAGE.
   *
   * @deprecated This library will not be creating or managing token contracts themselves anymore.
   */
  @Deprecated
  public static String getContractType(String contractAddress, Configuration config) {
    try {
      String contractCode = ethereumRpc.eth_getCode("0x" + contractAddress.toLowerCase(Locale.US),
          DefaultBlock.LATEST.toString());
      contractCode = contractCode.substring(2);
      Class<?> contractType = Contract.class;
      while (ContractInterface.class.isAssignableFrom(contractType)) {
        ContractInterface contractParams = (ContractInterface) contractType.newInstance();
        if (contractParams.getAdminRuntime().equalsIgnoreCase(contractCode)) {
          return Base.ADMIN;
        } else if (contractParams.getTokenRuntime().equalsIgnoreCase(contractCode)) {
          return Base.TOKEN;
        } else if (contractParams.getStorageRuntime().equalsIgnoreCase(contractCode)) {
          return Base.STORAGE;
        } else if (config.useAlternateEtherContract() && contractParams.getAlternateStorageRunTime()
            .equalsIgnoreCase(contractCode)) {
          return Base.STORAGE;
        }
        contractType = contractType.getSuperclass();
      }
    } catch (Exception e) {
      LOGGER.debug(null, e);
      return null;
    }

    return null;
  }

  /**
   * Determines what version of contract bytecode you're running at the provided address.
   */
  public static ContractInterface getContractVersion(String contractAddress, Configuration config) {
    try {
      String contractCode = ethereumRpc.eth_getCode("0x" + contractAddress.toLowerCase(Locale.US),
          DefaultBlock.LATEST.toString());
      contractCode = contractCode.substring(2);
      Class<?> contractType = Contract.class;
      while (ContractInterface.class.isAssignableFrom(contractType)) {
        ContractInterface contractParams = (ContractInterface) contractType.newInstance();
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

  /**
   * Creates a transaction with the appropriate data to mint new tokens.
   *
   * @deprecated This library will no longer support token contract management going forward.
   */
  @Deprecated
  public static String generateTokens(String recipient, long amount, Configuration config) {
    Long nonce = config.getContractInterface().getContractParameters()
        .getNonce(ethereumRpc, config.getAdminContractAddress(), config.getAdminAccount());
    RawTransaction tx = RawTransaction
        .createTransaction(config, config.getAdminContractAddress(), null,
            config.getContractInterface().getContractParameters()
                .createTokens(nonce, recipient, amount, new LinkedList<>(), new LinkedList<>(),
                    new LinkedList<>()));

    return ByteUtilities.toHexString(tx.encode());
  }

  /**
   * Creates a transaction with the appropriate data to destroy tokens.
   *
   * @deprecated This library will no longer support token contract management going forward.
   */
  @Deprecated
  public static String destroyTokens(String sender, long amount, Configuration config) {
    Long nonce = config.getContractInterface().getContractParameters()
        .getNonce(ethereumRpc, config.getAdminContractAddress(), config.getAdminAccount());
    RawTransaction tx = RawTransaction
        .createTransaction(config, config.getAdminContractAddress(), null,
            config.getContractInterface().getContractParameters()
                .destroyTokens(nonce, sender, amount, new LinkedList<>(), new LinkedList<>(),
                    new LinkedList<>()));

    return ByteUtilities.toHexString(tx.encode());
  }

  /**
   * Creates a transaction with the appropriate data to reconcile address balances.
   *
   * @param addressChanges Map of addresses and the change in balance to apply to them.
   */
  public static String reconcile(Map<String, BigInteger> addressChanges, Configuration config) {
    Long nonce = config.getContractInterface().getContractParameters()
        .getNonce(ethereumRpc, config.getStorageContractAddress(), config.getAdminAccount());
    RawTransaction tx = RawTransaction
        .createTransaction(config, config.getStorageContractAddress(), null,
            config.getContractInterface().getContractParameters()
                .reconcile(nonce, addressChanges, new LinkedList<>(), new LinkedList<>(),
                    new LinkedList<>()));

    return ByteUtilities.toHexString(tx.encode());
  }

  /**
   * Generates a transaction listing the allowance granted to the grantee based on the ERC-20
   * specification.
   *
   * @deprecated This library will no longer support token contract management going forward.
   */
  @Deprecated
  public static String allowance(String owner, String grantee, Configuration config) {
    CallData callData = EthereumTools.generateCall(
        config.getContractInterface().getContractParameters().allowance(owner, grantee),
        config.getTokenContractAddress());
    LOGGER.debug("Balance request: " + Json.stringifyObject(CallData.class, callData));

    return ethereumRpc.eth_call(callData, DefaultBlock.LATEST.toString());
  }

  /**
   * Generates a transaction granting an allowance based on the ERC-20 specification.
   *
   * @deprecated This library will no longer support token contract management going forward.
   */
  @Deprecated
  public static String approve(String grantee, BigDecimal amount, Configuration config) {
    RawTransaction tx = RawTransaction
        .createTransaction(config, config.getTokenContractAddress(), null,
            config.getContractInterface().getContractParameters().approve(grantee,
                amount.multiply(BigDecimal.TEN.pow((int) config.getDecimalPlaces()))
                    .toBigInteger()));

    return ByteUtilities.toHexString(tx.encode());
  }
}
