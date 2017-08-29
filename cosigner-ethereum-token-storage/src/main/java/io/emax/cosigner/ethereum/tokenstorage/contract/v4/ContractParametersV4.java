package io.emax.cosigner.ethereum.tokenstorage.contract.v4;

import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.ethereum.core.common.EthereumTools;
import io.emax.cosigner.ethereum.core.gethrpc.DefaultBlock;
import io.emax.cosigner.ethereum.core.gethrpc.EthereumRpc;
import io.emax.cosigner.ethereum.tokenstorage.Base;
import io.emax.cosigner.ethereum.tokenstorage.Configuration;
import io.emax.cosigner.ethereum.tokenstorage.contract.ContractInterface;
import io.emax.cosigner.ethereum.tokenstorage.contract.v3.ContractParametersV3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ContractParametersV4 extends ContractParametersV3 {
  private static final Logger LOGGER = LoggerFactory.getLogger(ContractParametersV4.class);

  @Override
  public Long getNonce(EthereumRpc ethereumRpc, String contractAddress, String senderAddress) {
    ContractV4 contract = new ContractV4();
    String txCount = ethereumRpc
        .eth_call(EthereumTools.generateCall(contract.getNonce(), contractAddress),
            DefaultBlock.LATEST.toString());
    LOGGER.debug("Getting nonce for 0x" + contractAddress + ": " + txCount);
    BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount));

    return nonce.longValue();
  }

  @Override
  public Long getSecurityValue(EthereumRpc ethereumRpc, String contractAddress) {
    return 0L;
  }

  @Override
  public String calculateAdminHash(EthereumRpc ethereumRpc, String contractAddress) {
    return calculateAdminHash(ethereumRpc, contractAddress, null);
  }

  @Override
  public String calculateAdminHash(EthereumRpc ethereumRpc, String contractAddress, Long nonce) {
    ContractV4 contract = new ContractV4();
    String response = ethereumRpc
        .eth_call(EthereumTools.generateCall(contract.getGetAdminHash(), contractAddress),
            DefaultBlock.LATEST.toString());
    LOGGER.debug("Getting admin hash for 0x" + contractAddress);

    return response;
  }

  @Override
  public String calculateTxHash(EthereumRpc ethereumRpc, String contractAddress, String sender,
      List<String> recipients, List<String> amounts) {
    return calculateTxHash(contractAddress, getNonce(ethereumRpc, contractAddress, sender), sender,
        recipients, amounts);
  }

  @Override
  public String calculateTxHash(String contractAddress, Long nonce, String sender,
      List<String> recipients, List<String> amounts) {
    ContractV4 contract = new ContractV4();
    String request = contract.getGetTxHash();

    request += padHex(sender);

    long numberOfParams = 3;
    long sizeOfPreviousArrays = 0;
    request += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    sizeOfPreviousArrays = recipients.size() + 1;
    request += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    //sizeOfPreviousArrays = amounts.size() + 1;

    request += serializeStringList(recipients);
    request += serializeBigIntStringList(amounts);

    LOGGER.debug("Requesting TX Hash: " + request);

    String response = Base.ethereumRpc
        .eth_call(EthereumTools.generateCall(request, contractAddress),
            DefaultBlock.LATEST.toString());
    LOGGER.debug("Getting TX hash");

    return response;
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
    return "";
  }

  @Override
  public String createTokenContract(String parentAddress) {
    return createTokenContract(parentAddress, null, null, 0);
  }

  @Override
  public String createTokenContract(String parentAddress, String name, String symbol,
      int decimals) {
    return "";
  }

  @Override
  public String createStorageContract(Configuration config, String tokenContract,
      String adminAddress, List<String> ownerAddresses, int numSignaturesRequired) {
    return createStorageContract(config, tokenContract, adminAddress, ownerAddresses,
        numSignaturesRequired, new Random().nextLong(), null, null, 0);
  }

  @Override
  public String createStorageContract(Configuration config, String tokenContract,
      String adminAddress, List<String> ownerAddresses, int numSignaturesRequired,
      long securityValue, String name, String symbol, int decimals) {
    // Format should be initData/Payload/Arguments
    ContractV4 contract = new ContractV4();
    String response = contract.getStorageInitData();
    if (config.useAlternateEtherContract()) {
      response = contract.getAlternateStorageInitData();
    }

    // Now gather the arguments and serialize them.
    // tokenContract
    response += padHex(tokenContract);

    // AdminAddress
    response += padHex(adminAddress);

    long numberOfParams = 7;
    long sizeOfPreviousArrays = 0;
    // OwnerAddress pointer (array)
    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    sizeOfPreviousArrays += ownerAddresses.size() + 1;

    // NumSignaturesRequired
    response +=
        padHex(ByteUtilities.toHexString(BigInteger.valueOf(numSignaturesRequired).toByteArray()));

    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    sizeOfPreviousArrays += serializeString(name).length() / 64;

    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));

    response += padHex(ByteUtilities.toHexString(BigInteger.valueOf(decimals).toByteArray()));

    response += serializeStringList(ownerAddresses);
    response += serializeString(name);
    response += serializeString(symbol);

    return response;
  }

  @Override
  public String setTokenChild(long nonce, String childAddress, List<String> sigV, List<String> sigR,
      List<String> sigS) {
    // Contract data
    ContractV4 contract = new ContractV4();
    String response = contract.getSetTokenContract();

    // Arguments
    response += padHex(childAddress);

    long numberOfParams = 4;
    long sizeOfPreviousArrays = 0;

    // SigV Pointer
    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    sizeOfPreviousArrays += sigV.size() + 1;

    // SigR Pointer
    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    sizeOfPreviousArrays += sigR.size() + 1;

    // SigS Pointer
    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));

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
    return "";
  }

  @Override
  public String destroyTokens(long nonce, String sender, long numTokens, List<String> sigV,
      List<String> sigR, List<String> sigS) {
    return "";
  }

  @Override
  public String reconcile(long nonce, Map<String, BigInteger> addressChanges, List<String> sigV,
      List<String> sigR, List<String> sigS) {

    LOGGER.debug("Asked to reconcile: " + Json.stringifyObject(Map.class, addressChanges));

    // Contract data
    ContractV4 contract = new ContractV4();
    String response = contract.getReconcile();

    // Arguments
    long numberOfParams = 5;
    long sizeOfPreviousArrays = 0;
    List<String> addresses = new LinkedList<>(addressChanges.keySet());
    List<BigInteger> balanceChanges = new LinkedList<>();
    addresses.forEach(address -> {
      balanceChanges.add(addressChanges.get(address));
    });

    // Address Pointer
    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    sizeOfPreviousArrays += addresses.size() + 1;

    // Balance Pointer
    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    sizeOfPreviousArrays += balanceChanges.size() + 1;

    // SigV Pointer
    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    sizeOfPreviousArrays += sigV.size() + 1;

    // SigR Pointer
    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    sizeOfPreviousArrays += sigR.size() + 1;

    // SigS Pointer
    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));

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
    ContractV4 contract = new ContractV4();
    String response = contract.getGetBalance();

    response += padHex(address);

    return response;
  }

  @Override
  public String getTotalBalance() {
    ContractV4 contract = new ContractV4();

    return contract.getGetTotalBalance();
  }

  @Override
  public String deposit(Configuration config, String recipient, BigInteger amount) {
    ContractV4 contract = new ContractV4();
    String response = contract.getDeposit();

    if (config.useAlternateEtherContract()) {
      response = contract.getAlternateDeposit();
    }

    response += padHex(recipient);

    if (!config.useAlternateEtherContract()) {
      response += padHex(ByteUtilities.toHexString(amount.toByteArray()));
    }

    return response;
  }

  @Override
  public String tokenTransfer(String recipient, BigInteger amount) {
    ContractV4 contract = new ContractV4();
    String response = contract.getTokenTransfer();

    response += padHex(recipient);
    response += padHex(ByteUtilities.toHexString(amount.toByteArray()));

    return response;
  }

  @Override
  public String offlineTransfer(long nonce, String sender, List<String> recipients,
      List<BigInteger> amount, List<String> sigV, List<String> sigR, List<String> sigS) {
    // We expect the list of sig's to be in order such that the index of each matches up.
    // We expect that the recipients and amounts be in order such that the index of each matches up.
    // I.E. sigV[2] belongs with sigR[2] and sigS[2].

    ContractV4 contract = new ContractV4();
    String response = contract.getTransfer();

    // Sender
    response += padHex(sender);

    long numberOfParams = 6;
    long sizeOfPreviousArrays = 0;

    // Recipient Pointer
    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    sizeOfPreviousArrays += recipients.size() + 1;

    // Amount Pointer
    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    sizeOfPreviousArrays += amount.size() + 1;

    // SigV Pointer
    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    sizeOfPreviousArrays += sigV.size() + 1;

    // SigR Pointer
    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    sizeOfPreviousArrays += sigR.size() + 1;

    // SigS Pointer
    response += padHex(calculatePointer(numberOfParams, sizeOfPreviousArrays));
    //sizeOfPreviousArrays += sigS.size() + 1;

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
    ContractV4 contract = new ContractV4();
    String response = contract.getApprove();

    // Grantee
    response += padHex(grantee);

    // Amount
    response += serializeBigInt(amount);

    return response;
  }

  @Override
  public String allowance(String owner, String grantee) {
    ContractV4 contract = new ContractV4();
    String response = contract.getAllowance();

    // owner
    response += padHex(owner);

    // Grantee
    response += padHex(grantee);

    return response;
  }

  private String calculatePointer(long numberOfParams, long sizeOfPreviousArrays) {
    return ByteUtilities.toHexString(
        BigInteger.valueOf(32 * (numberOfParams + sizeOfPreviousArrays)).toByteArray());
  }

  private String padHex(String hexData, char pad) {
    return String.format("%64s", hexData).replace(' ', pad);
  }

  private String padHex(String hexData) {
    return padHex(hexData, '0');
  }

  private String serializeString(String data) {
    String response = "";

    try {
      data = ByteUtilities.toHexString(data.getBytes("UTF-8"));
    } catch (Exception e) {
      LOGGER.error("Couldn't serialize String: " + data, e);
      return padHex("");
    }

    String formattedString =
        ByteUtilities.toHexString(BigInteger.valueOf(data.length()).toByteArray());
    formattedString = padHex(formattedString);
    response += formattedString;

    while (data.length() >= 64) {
      response += data.substring(0, 64);
      data = data.substring(64);
    }

    if (data.length() > 0) {
      formattedString = padHex("");
      formattedString = data + formattedString;
      response += formattedString.substring(0, 64);
    }

    return response;
  }

  private String serializeStringList(List<String> data) {
    String response = "";

    String formattedString =
        ByteUtilities.toHexString(BigInteger.valueOf(data.size()).toByteArray());
    formattedString = padHex(formattedString);
    response += formattedString;

    for (String dataItem : data) {
      formattedString = padHex(dataItem);
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
    formattedString = padHex(formattedString, pad);
    return formattedString;
  }

  private String serializeBigIntList(List<BigInteger> data) {
    String response = "";

    String formattedString =
        ByteUtilities.toHexString(BigInteger.valueOf(data.size()).toByteArray());
    formattedString = padHex(formattedString);
    response += formattedString;

    for (BigInteger dataItem : data) {
      response += serializeBigInt(dataItem);
    }

    return response;
  }

  private String serializeBigIntStringList(List<String> data) {
    String response = "";

    String formattedString =
        ByteUtilities.toHexString(BigInteger.valueOf(data.size()).toByteArray());
    formattedString = padHex(formattedString);
    response += formattedString;

    for (String dataItem : data) {
      response += serializeBigInt(new BigInteger(dataItem));
    }

    return response;
  }

  @Override
  public Map<String, List<String>> parseTransfer(String bytecode) {
    if (bytecode == null || bytecode.length() < 8) {
      return null;
    }

    ContractV4 contract = new ContractV4();
    int bufPointer = 0;
    final int paramSize = 64;
    String readValue;

    // Check that this is a offlineTransfer command.
    String function = bytecode.substring(bufPointer, bufPointer + contract.getTransfer().length());
    bufPointer += contract.getTransfer().length();
    LOGGER.debug("Found function: " + function);
    if (!function.equalsIgnoreCase(contract.getTransfer())) {
      return null;
    }

    HashMap<String, List<String>> parameters = new HashMap<>();
    parameters.put(NONCE, Collections.singletonList("0"));

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
    ContractV4 contract = new ContractV4();
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

    parameters.put(NONCE, Collections.singletonList("0"));

    // Params
    for (int i = 0; i < contract.getAdminParamTypes().get(function.toLowerCase()).size(); i++) {
      if (contract.getAdminParamTypes().get(function.toLowerCase()).get(i)
          .equalsIgnoreCase(ContractInterface.ValueParam)) {
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
      case ContractV4.createTokens:
        result = createTokens(new BigInteger(params.get(NONCE).get(0)).longValue(),
            params.get(PARAM + 0).get(0),
            new BigInteger(1, ByteUtilities.toByteArray(params.get(PARAM + 1).get(0))).longValue(),
            params.get(SIGV), params.get(SIGR), params.get(SIGS));
        break;
      case ContractV4.destroyTokens:
        result = destroyTokens(new BigInteger(params.get(NONCE).get(0)).longValue(),
            params.get(PARAM + 0).get(0),
            new BigInteger(1, ByteUtilities.toByteArray(params.get(PARAM + 1).get(0))).longValue(),
            params.get(SIGV), params.get(SIGR), params.get(SIGS));
        break;
      case ContractV4.setTokenContract:
        result = setTokenChild(new BigInteger(params.get(NONCE).get(0)).longValue(),
            params.get(PARAM + 0).get(0), params.get(SIGV), params.get(SIGR), params.get(SIGS));
        break;
      case ContractV4.reconcile:
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
    arrayPointer +=
        new ContractV4().getTransfer().length(); // Offset by function id, not included in calc.

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
