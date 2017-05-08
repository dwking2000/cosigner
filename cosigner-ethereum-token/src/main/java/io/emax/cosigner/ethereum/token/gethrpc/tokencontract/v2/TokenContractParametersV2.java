package io.emax.cosigner.ethereum.token.gethrpc.tokencontract.v2;

import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.ethereum.core.common.EthereumTools;
import io.emax.cosigner.ethereum.core.gethrpc.DefaultBlock;
import io.emax.cosigner.ethereum.core.gethrpc.EthereumRpc;
import io.emax.cosigner.ethereum.token.TokenConfiguration;
import io.emax.cosigner.ethereum.token.gethrpc.tokencontract.v1.TokenContractParametersV1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.emax.cosigner.ethereum.token.gethrpc.tokencontract.TokenContractInterface.ValueParam;

public class TokenContractParametersV2 extends TokenContractParametersV1 {
  private static final Logger LOGGER = LoggerFactory.getLogger(TokenContractParametersV2.class);

  @Override
  public Long getNonce(EthereumRpc ethereumRpc, String contractAddress) {
    TokenContractV2 contract = new TokenContractV2();
    String txCount = ethereumRpc
        .eth_call(EthereumTools.generateCall(contract.getNonce(), contractAddress),
            DefaultBlock.LATEST.toString());
    LOGGER.debug("Getting nonce for 0x" + contractAddress + ": " + txCount);
    BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount));

    return nonce.longValue();
  }

  public Long getSecurityValue(EthereumRpc ethereumRpc, String contractAddress) {
    TokenContractV2 contract = new TokenContractV2();
    String secValueStr = ethereumRpc
        .eth_call(EthereumTools.generateCall(contract.getSecurityValue(), contractAddress),
            DefaultBlock.LATEST.toString());

    LOGGER.debug("Getting securityValue for 0x" + contractAddress + ": " + secValueStr);
    BigInteger securityValue = new BigInteger(1, ByteUtilities.toByteArray(secValueStr));

    return securityValue.longValue();
  }

  @Override
  public String calculateAdminHash(EthereumRpc ethereumRpc, String contractAddress) {
    return calculateAdminHash(ethereumRpc, contractAddress, getNonce(ethereumRpc, contractAddress));
  }

  @Override
  public String calculateAdminHash(EthereumRpc ethereumRpc, String contractAddress, Long nonce) {
    String formattedString = ByteUtilities.toHexString(BigInteger.valueOf(nonce).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    String response = formattedString;

    formattedString = ByteUtilities.toHexString(
        BigInteger.valueOf(getSecurityValue(ethereumRpc, contractAddress)).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    LOGGER.debug("Hashing value: " + response);
    response = EthereumTools.hashKeccak(response);
    LOGGER.debug("Calculated Admin Hash: " + response);

    return response;
  }

  @Override
  public String calculateTxHash(EthereumRpc ethereumRpc, String contractAddress,
      List<String> recipients, List<String> amounts) {
    return calculateTxHash(getNonce(ethereumRpc, contractAddress), recipients, amounts);
  }

  @Override
  public String calculateTxHash(Long nonce, List<String> recipients, List<String> amounts) {
    String hashBytes = String.format("%64s", "0").replace(' ', '0');
    for (int i = 0; i < recipients.size(); i++) {
      hashBytes += String.format("%40s", recipients.get(i)).replace(' ', '0');
      hashBytes += String
          .format("%64s", ByteUtilities.toHexString(new BigInteger(amounts.get(i)).toByteArray()))
          .replace(' ', '0');
      hashBytes +=
          String.format("%64s", ByteUtilities.toHexString(BigInteger.valueOf(nonce).toByteArray()))
              .replace(' ', '0');

      LOGGER.debug("Hashing: " + hashBytes);
      hashBytes = EthereumTools.hashKeccak(hashBytes);
      LOGGER.debug("Result: " + hashBytes);
    }

    LOGGER.debug("Got TX Hash: " + hashBytes);
    return hashBytes;
  }

  @Override
  public String createAdminContract(String adminAddress, List<String> ownerAddresses,
      int numSignaturesRequired) {
    return createAdminContract(adminAddress, ownerAddresses, numSignaturesRequired,
        new Random().nextLong());
  }

  @Override
  public String createAdminContract(String adminAddress, List<String> ownerAddresses,
      int numSignaturesRequired, long securityValue) {
    // Format should be initData/Payload/Arguments
    TokenContractV2 contract = new TokenContractV2();
    String response = contract.getAdminInitData();

    // Now gather the arguments and serialize them.
    // AdminAddress
    String formattedString = String.format("%64s", adminAddress).replace(' ', '0');
    response += formattedString;

    long numberOfParams = 4;
    long sizeOfPreviousArrays = 0;
    // OwnerAddress pointer (array)
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;

    // NumSignaturesRequired
    formattedString =
        ByteUtilities.toHexString(BigInteger.valueOf(numSignaturesRequired).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    // securityValue
    formattedString = ByteUtilities.toHexString(BigInteger.valueOf(securityValue).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    // OwnerAddress data
    response += serializeStringList(ownerAddresses);

    return response;
  }

  @Override
  public String createTokenContract(String parentAddress) {
    return createTokenContract(parentAddress, null, null, 0);
  }

  @Override
  public String createTokenContract(String parentAddress, String name, String symbol,
      int decimals) {
    // Contract data
    TokenContractV2 contract = new TokenContractV2();
    String response = contract.getTokenInitData();

    // Arguments
    String formattedString = String.format("%64s", parentAddress).replace(' ', '0');
    response += formattedString;

    long numberOfParams = 4;
    long sizeOfPreviousArrays = 0;
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += serializeString(name).length() / 64;

    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;

    formattedString = ByteUtilities.toHexString(BigInteger.valueOf(decimals).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    response += serializeString(name);
    response += serializeString(symbol);

    return response;
  }

  @Override
  public String createStorageContract(TokenConfiguration config, String tokenContract,
      String adminAddress, List<String> ownerAddresses, int numSignaturesRequired) {
    return createStorageContract(config, tokenContract, adminAddress, ownerAddresses,
        numSignaturesRequired, new Random().nextLong(), null, null, 0);
  }

  @Override
  public String createStorageContract(TokenConfiguration config, String tokenContract,
      String adminAddress, List<String> ownerAddresses, int numSignaturesRequired,
      long securityValue, String name, String symbol, int decimals) {
    // Format should be initData/Payload/Arguments
    TokenContractV2 contract = new TokenContractV2();
    String response = contract.getStorageInitData();
    if (config.useAlternateEtherContract()) {
      response = contract.getAlternateStorageInitData();
    }

    // Now gather the arguments and serialize them.
    // tokenContract
    String formattedString = String.format("%64s", tokenContract).replace(' ', '0');
    response += formattedString;

    // AdminAddress
    formattedString = String.format("%64s", adminAddress).replace(' ', '0');
    response += formattedString;

    long numberOfParams = 8;
    long sizeOfPreviousArrays = 0;
    // OwnerAddress pointer (array)
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += ownerAddresses.size() + 1;

    // NumSignaturesRequired
    formattedString =
        ByteUtilities.toHexString(BigInteger.valueOf(numSignaturesRequired).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    // securityValue
    formattedString = ByteUtilities.toHexString(BigInteger.valueOf(securityValue).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += serializeString(name).length() / 64;

    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;

    formattedString = ByteUtilities.toHexString(BigInteger.valueOf(decimals).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    response += serializeStringList(ownerAddresses);
    response += serializeString(name);
    response += serializeString(symbol);

    return response;
  }

  @Override
  public String setTokenChild(long nonce, String childAddress, List<String> sigV, List<String> sigR,
      List<String> sigS) {
    // Contract data
    TokenContractV2 contract = new TokenContractV2();
    String response = contract.getSetTokenContract();

    // Arguments
    String formattedString = ByteUtilities.toHexString(BigInteger.valueOf(nonce).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    formattedString = String.format("%64s", childAddress).replace(' ', '0');
    response += formattedString;

    long numberOfParams = 5;
    long sizeOfPreviousArrays = 0;

    // SigV Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += sigV.size() + 1;

    // SigR Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += sigR.size() + 1;

    // SigS Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;

    // SigV Data
    response += serializeStringList(sigV);

    // SigR Data
    response += serializeStringList(sigR);

    // SigS Data
    response += serializeStringList(sigS);

    return response;
  }

  @Override
  public String createTokens(long nonce, String recipient, long numTokens, List<String> sigV,
      List<String> sigR, List<String> sigS) {
    // Contract data
    TokenContractV2 contract = new TokenContractV2();
    String response = contract.getCreateTokens();

    // Arguments
    String formattedString = ByteUtilities.toHexString(BigInteger.valueOf(nonce).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    formattedString = String.format("%64s", recipient).replace(' ', '0');
    response += formattedString;

    formattedString = ByteUtilities.toHexString(BigInteger.valueOf(numTokens).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    long numberOfParams = 6;
    long sizeOfPreviousArrays = 0;

    // SigV Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += sigV.size() + 1;

    // SigR Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += sigR.size() + 1;

    // SigS Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;

    // SigV Data
    response += serializeStringList(sigV);

    // SigR Data
    response += serializeStringList(sigR);

    // SigS Data
    response += serializeStringList(sigS);

    return response;
  }

  @Override
  public String destroyTokens(long nonce, String sender, long numTokens, List<String> sigV,
      List<String> sigR, List<String> sigS) {
    // Contract data
    TokenContractV2 contract = new TokenContractV2();
    String response = contract.getDestroyTokens();

    // Arguments
    String formattedString = ByteUtilities.toHexString(BigInteger.valueOf(nonce).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    formattedString = String.format("%64s", sender).replace(' ', '0');
    response += formattedString;

    formattedString = ByteUtilities.toHexString(BigInteger.valueOf(numTokens).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    long numberOfParams = 6;
    long sizeOfPreviousArrays = 0;

    // SigV Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += sigV.size() + 1;

    // SigR Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += sigR.size() + 1;

    // SigS Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;

    // SigV Data
    response += serializeStringList(sigV);

    // SigR Data
    response += serializeStringList(sigR);

    // SigS Data
    response += serializeStringList(sigS);

    return response;
  }

  @Override
  public String reconcile(long nonce, Map<String, BigInteger> addressChanges, List<String> sigV,
      List<String> sigR, List<String> sigS) {

    LOGGER.debug("Asked to reconcile: " + Json.stringifyObject(Map.class, addressChanges));

    // Contract data
    TokenContractV2 contract = new TokenContractV2();
    String response = contract.getReconcile();

    // Arguments
    String formattedString = ByteUtilities.toHexString(BigInteger.valueOf(nonce).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    long numberOfParams = 6;
    long sizeOfPreviousArrays = 0;
    List<String> addresses = new LinkedList<>(addressChanges.keySet());
    List<BigInteger> balanceChanges = new LinkedList<>();
    addresses.forEach(address -> {
      balanceChanges.add(addressChanges.get(address));
    });

    // Address Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += addresses.size() + 1;

    // Balance Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += balanceChanges.size() + 1;

    // SigV Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += sigV.size() + 1;

    // SigR Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += sigR.size() + 1;

    // SigS Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;

    // Address Data
    response += serializeStringList(addresses);

    // Balance Data
    response += serializeBigIntList(balanceChanges);

    // SigV Data
    response += serializeStringList(sigV);

    // SigR Data
    response += serializeStringList(sigR);

    // SigS Data
    response += serializeStringList(sigS);

    return response;
  }

  @Override
  public String getBalance(String address) {
    TokenContractV2 contract = new TokenContractV2();
    String response = contract.getGetBalance();

    String formattedString = String.format("%64s", address).replace(' ', '0');
    response += formattedString;

    return response;
  }

  @Override
  public String getTotalBalance() {
    TokenContractV2 contract = new TokenContractV2();

    return contract.getGetTotalBalance();
  }

  @Override
  public String deposit(TokenConfiguration config, String recipient, BigInteger amount) {
    TokenContractV2 contract = new TokenContractV2();
    String response = contract.getDeposit();

    if (config.useAlternateEtherContract()) {
      response = contract.getAlternateDeposit();
    }

    String formattedString = String.format("%64s", recipient).replace(' ', '0');
    response += formattedString;

    if (!config.useAlternateEtherContract()) {
      formattedString = ByteUtilities.toHexString(amount.toByteArray());
      formattedString = String.format("%64s", formattedString).replace(' ', '0');
      response += formattedString;
    }

    return response;
  }

  @Override
  public String tokenTransfer(String recipient, BigInteger amount) {
    TokenContractV2 contract = new TokenContractV2();
    String response = contract.getTokenTransfer();

    String formattedString = String.format("%64s", recipient).replace(' ', '0');
    response += formattedString;

    formattedString = ByteUtilities.toHexString(amount.toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    return response;
  }

  @Override
  public String transfer(long nonce, String sender, List<String> recipients,
      List<BigInteger> amount, List<String> sigV, List<String> sigR, List<String> sigS) {
    // We expect the list of sig's to be in order such that the index of each matches up.
    // We expect that the recipients and amounts be in order such that the index of each matches up.
    // I.E. sigV[2] belongs with sigR[2] and sigS[2].

    TokenContractV2 contract = new TokenContractV2();
    String response = contract.getTransfer();

    // Nonce
    String formattedString = ByteUtilities.toHexString(BigInteger.valueOf(nonce).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    // Sender
    formattedString = String.format("%64s", sender).replace(' ', '0');
    response += formattedString;

    long numberOfParams = 7;
    long sizeOfPreviousArrays = 0;

    // Recipient Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += recipients.size() + 1;

    // Amount Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += amount.size() + 1;

    // SigV Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += sigV.size() + 1;

    // SigR Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;
    sizeOfPreviousArrays += sigR.size() + 1;

    // SigS Pointer
    formattedString = String.format("%64s", calculatePointer(numberOfParams, sizeOfPreviousArrays))
        .replace(' ', '0');
    response += formattedString;

    // Recipient Data
    response += serializeStringList(recipients);

    // Amount Data
    response += serializeBigIntList(amount);

    // SigV Data
    response += serializeStringList(sigV);

    // SigR Data
    response += serializeStringList(sigR);

    // SigS Data
    response += serializeStringList(sigS);

    return response;
  }

  @Override
  public String approve(String grantee, BigInteger amount) {
    TokenContractV2 contract = new TokenContractV2();
    String response = contract.getApprove();

    // Grantee
    String formattedString = String.format("%64s", grantee).replace(' ', '0');
    response += formattedString;

    // Amount
    formattedString = serializeBigInt(amount);
    response += formattedString;

    return response;
  }

  @Override
  public String allowance(String owner, String grantee) {
    TokenContractV2 contract = new TokenContractV2();
    String response = contract.getAllowance();

    // owner
    String formattedString = String.format("%64s", owner).replace(' ', '0');
    response += formattedString;

    // Grantee
    formattedString = String.format("%64s", grantee).replace(' ', '0');
    response += formattedString;

    return response;
  }

  private String calculatePointer(long numberOfParams, long sizeOfPreviousArrays) {
    return ByteUtilities.toHexString(
        BigInteger.valueOf(32 * (numberOfParams + sizeOfPreviousArrays)).toByteArray());
  }

  private String serializeString(String data) {
    String response = "";

    try {
      data = ByteUtilities.toHexString(data.getBytes("UTF-8"));
    } catch (Exception e) {
      LOGGER.error("Couldn't serialize String: " + data, e);
      return String.format("%64s", "").replace(' ', '0');
    }

    String formattedString =
        ByteUtilities.toHexString(BigInteger.valueOf(data.length()).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    while (data.length() >= 64) {
      response += data.substring(0, 64);
      data = data.substring(64);
    }

    if (data.length() > 0) {
      formattedString = String.format("%64s", "").replace(' ', '0');
      formattedString = data + formattedString;
      response += formattedString.substring(0, 64);
    }

    return response;
  }

  private String serializeStringList(List<String> data) {
    String response = "";

    String formattedString =
        ByteUtilities.toHexString(BigInteger.valueOf(data.size()).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    for (String dataItem : data) {
      formattedString = String.format("%64s", dataItem).replace(' ', '0');
      response += formattedString;
    }

    return response;
  }

  private String serializeBigInt(BigInteger data) {
    String formattedString;
    char pad = '0';
    LOGGER.debug("Serializing " + data.toString());
    if (data.compareTo(BigInteger.ZERO) < 0) {
      pad = 'F';
    }
    LOGGER.debug("Using pad " + pad);
    formattedString = ByteUtilities.toHexString(data.toByteArray());
    LOGGER.debug("Got bytes: " + formattedString);
    formattedString = String.format("%64s", formattedString).replace(' ', pad);
    return formattedString;
  }

  private String serializeBigIntList(List<BigInteger> data) {
    String response = "";

    String formattedString =
        ByteUtilities.toHexString(BigInteger.valueOf(data.size()).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    for (BigInteger dataItem : data) {
      response += serializeBigInt(dataItem);
    }

    return response;
  }

  @Override
  public Map<String, List<String>> parseTransfer(String bytecode) {
    if (bytecode == null || bytecode.length() < 8) {
      return null;
    }

    TokenContractV2 contract = new TokenContractV2();
    int bufPointer = 0;
    final int paramSize = 64;
    String readValue;

    // Check that this is a transfer command.
    String function = bytecode.substring(bufPointer, bufPointer + contract.getTransfer().length());
    bufPointer += contract.getTransfer().length();
    LOGGER.debug("Found function: " + function);
    if (!function.equalsIgnoreCase(contract.getTransfer())) {
      return null;
    }

    // Nonce
    readValue = bytecode.substring(bufPointer, bufPointer + paramSize);
    bufPointer += paramSize;
    BigInteger intValue = new BigInteger("00" + readValue, 16);
    HashMap<String, List<String>> parameters = new HashMap<>();
    parameters.put(NONCE, Collections.singletonList(intValue.toString(10)));

    // Sender
    readValue = bytecode.substring(bufPointer, bufPointer + paramSize);
    bufPointer += paramSize;
    readValue = ByteUtilities
        .toHexString(ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(readValue)));
    parameters.put(SENDER, Collections.singletonList(readValue));

    // Recipient
    parameters.put(RECIPIENTS, parseArray(bufPointer, bytecode, false, true));
    bufPointer += paramSize;

    // Amount
    parameters.put(AMOUNT, parseArray(bufPointer, bytecode, true, true));
    bufPointer += paramSize;

    // SigV
    parameters.put(SIGV, parseArray(bufPointer, bytecode, false, true));
    bufPointer += paramSize;

    // SigR
    parameters.put(SIGR, parseArray(bufPointer, bytecode, false, true));
    bufPointer += paramSize;

    // SigS
    parameters.put(SIGS, parseArray(bufPointer, bytecode, false, true));

    return parameters;
  }

  @Override
  public Map<String, List<String>> parseAdminFunction(String bytecode) {
    if (bytecode == null || bytecode.length() < 8) {
      return null;
    }
    TokenContractV2 contract = new TokenContractV2();
    int bufPointer = 0;
    final int paramSize = 64;
    String readValue;

    // Check that this is an admin function.
    String function = bytecode.substring(bufPointer, bufPointer + contract.getTransfer().length());
    bufPointer += contract.getTransfer().length();
    HashMap<String, List<String>> parameters = new HashMap<>();
    parameters.put(FUNCTION, Collections.singletonList(function));
    LOGGER.debug("Found function: " + function);
    boolean isAdminFunction = false;
    for (String adminFunction : contract.getAdminParamTypes().keySet()) {
      if (adminFunction.equalsIgnoreCase(function)) {
        isAdminFunction = true;
      }
    }
    if (!isAdminFunction) {
      return null;
    }

    // Nonce
    readValue = bytecode.substring(bufPointer, bufPointer + paramSize);
    bufPointer += paramSize;
    BigInteger intValue = new BigInteger("00" + readValue, 16);
    parameters.put(NONCE, Collections.singletonList(intValue.toString(10)));

    // Params
    for (int i = 0; i < contract.getAdminParamTypes().get(function.toLowerCase()).size(); i++) {
      if (contract.getAdminParamTypes().get(function.toLowerCase()).get(i)
          .equalsIgnoreCase(ValueParam)) {
        readValue = bytecode.substring(bufPointer, bufPointer + paramSize);
        bufPointer += paramSize;
        readValue = ByteUtilities.toHexString(ByteUtilities.toByteArray(readValue));
        parameters.put(PARAM + i, Collections.singletonList(readValue));
      } else {
        parameters.put(PARAM + i, parseArray(bufPointer, bytecode, false, false));
        bufPointer += paramSize;
      }
    }

    // SigV
    parameters.put(SIGV, parseArray(bufPointer, bytecode, false, true));
    bufPointer += paramSize;

    // SigR
    parameters.put(SIGR, parseArray(bufPointer, bytecode, false, true));
    bufPointer += paramSize;

    // SigS
    parameters.put(SIGS, parseArray(bufPointer, bytecode, false, true));

    return parameters;
  }

  @Override
  public String rebuildAdminFunction(Map<String, List<String>> params) {
    String function = params.get(FUNCTION).get(0);
    String result;
    switch (function) {
      case TokenContractV2.createTokens:
        result = createTokens(new BigInteger(params.get(NONCE).get(0)).longValue(),
            params.get(PARAM + 0).get(0),
            new BigInteger(1, ByteUtilities.toByteArray(params.get(PARAM + 1).get(0))).longValue(),
            params.get(SIGV), params.get(SIGR), params.get(SIGS));
        break;
      case TokenContractV2.destroyTokens:
        result = destroyTokens(new BigInteger(params.get(NONCE).get(0)).longValue(),
            params.get(PARAM + 0).get(0),
            new BigInteger(1, ByteUtilities.toByteArray(params.get(PARAM + 1).get(0))).longValue(),
            params.get(SIGV), params.get(SIGR), params.get(SIGS));
        break;
      case TokenContractV2.setTokenContract:
        result = setTokenChild(new BigInteger(params.get(NONCE).get(0)).longValue(),
            params.get(PARAM + 0).get(0), params.get(SIGV), params.get(SIGR), params.get(SIGS));
        break;
      case TokenContractV2.reconcile:
        Map<String, BigInteger> balanceUpdates = new HashMap<>();
        for (int i = 0; i < params.get(PARAM + 0).size(); i++) {
          balanceUpdates.put(params.get(PARAM + 0).get(i),
              new BigInteger(ByteUtilities.toByteArray(params.get(PARAM + 1).get(i))));
        }
        result = reconcile(new BigInteger(params.get(NONCE).get(0)).longValue(), balanceUpdates,
            params.get(SIGV), params.get(SIGR), params.get(SIGS));
        break;
      default:
        result = null;
        break;
    }

    return result;
  }

  private List<String> parseArray(int bufPointer, String buffer, boolean decodeToInt,
      boolean stripLeadingZeros) {
    final int paramSize = 64;
    // Find the location of the array data
    String readValue = buffer.substring(bufPointer, bufPointer + paramSize);
    BigInteger intValue = new BigInteger("00" + readValue, 16);
    int arrayPointer = intValue.intValue() * 2; // String vs Bytes
    arrayPointer += new TokenContractV2().getTransfer()
        .length(); // Offset by function id, not included in calc.

    // Get the size of the array
    readValue = buffer.substring(arrayPointer, arrayPointer + paramSize);
    arrayPointer += paramSize;
    intValue = new BigInteger("00" + readValue, 16);
    int arraySize = intValue.intValue();

    // Read in the data
    LinkedList<String> arrayData = new LinkedList<>();
    for (int i = 0; i < arraySize; i++) {
      readValue = buffer.substring(arrayPointer, arrayPointer + paramSize);
      arrayPointer += paramSize;
      readValue = ByteUtilities.toHexString(stripLeadingZeros ?
          ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(readValue)) :
          ByteUtilities.toByteArray(readValue));

      // If we want the int value of the hex data
      if (decodeToInt) {
        intValue = new BigInteger("00" + readValue, 16);
        readValue = intValue.toString(10);
      }

      arrayData.add(readValue);
    }

    return arrayData;
  }
}
