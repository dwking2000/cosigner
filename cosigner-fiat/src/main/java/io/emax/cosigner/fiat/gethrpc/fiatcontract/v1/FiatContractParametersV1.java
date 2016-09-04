package io.emax.cosigner.fiat.gethrpc.fiatcontract.v1;

import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.fiat.gethrpc.fiatcontract.FiatContractParametersInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FiatContractParametersV1 implements FiatContractParametersInterface {
  private static final Logger LOGGER = LoggerFactory.getLogger(FiatContractParametersV1.class);

  @Override
  public String createContract(String adminAddress, List<String> ownerAddresses,
      int numSignaturesRequired) {
    // Format should be initData/Payload/Arguments
    FiatContractV1 contract = new FiatContractV1();
    String response = contract.getInitData() + contract.getContractPayload();

    // Now gather the arguments and serialize them.
    // AdminAddress
    String formattedString = String.format("%64s", adminAddress).replace(' ', '0');
    response += formattedString;

    // OwnerAddress pointer (array)
    // 32 (admin) + 32 (owner pointer) + 32 (required) = 96 = 0x60
    formattedString = String.format("%64s", "60").replace(' ', '0');
    response += formattedString;

    // NumSignaturesRequired
    formattedString =
        ByteUtilities.toHexString(BigInteger.valueOf(numSignaturesRequired).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    // OwnerAddress data
    // Size of array
    formattedString =
        ByteUtilities.toHexString(BigInteger.valueOf(ownerAddresses.size()).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;
    // Array data
    for (String address : ownerAddresses) {
      formattedString = String.format("%64s", address).replace(' ', '0');
      response += formattedString;
    }

    return response;
  }

  @Override
  public String createTokens(long numTokens) {
    FiatContractV1 contract = new FiatContractV1();
    String response = contract.getCreateTokens();

    String formattedString = ByteUtilities.toHexString(BigInteger.valueOf(numTokens).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    return response;
  }

  @Override
  public String destroyTokens(long numTokens) {
    FiatContractV1 contract = new FiatContractV1();
    String response = contract.getDestroyTokens();

    String formattedString = ByteUtilities.toHexString(BigInteger.valueOf(numTokens).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    return response;
  }

  @Override
  public String getOwners() {
    FiatContractV1 contract = new FiatContractV1();
    String response = contract.getGetOwners();

    return response;
  }

  @Override
  public String getBalance(String address) {
    FiatContractV1 contract = new FiatContractV1();
    String response = contract.getGetBalance();

    String formattedString = String.format("%64s", address).replace(' ', '0');
    response += formattedString;

    return response;
  }

  @Override
  public String getConfirmations(String address) {
    FiatContractV1 contract = new FiatContractV1();
    String response = contract.getGetConfirmations();

    String formattedString = String.format("%64s", address).replace(' ', '0');
    response += formattedString;

    return response;
  }

  @Override
  public String getTotalBalance() {
    FiatContractV1 contract = new FiatContractV1();
    String response = contract.getGetTotalBalance();

    return response;
  }

  @Override
  public String isOwner(String address) {
    FiatContractV1 contract = new FiatContractV1();
    String response = contract.getIsOwner();

    String formattedString = String.format("%64s", address).replace(' ', '0');
    response += formattedString;

    return response;
  }

  @Override
  public String transfer(long nonce, String sender, List<String> recipients, List<Long> amount,
      List<String> sigV, List<String> sigR, List<String> sigS) {
    // We expect the list of sig's to be in order such that the index of each matches up.
    // We expect that the recipients and amounts be in order such that the index of each matches up.
    // I.E. sigV[2] belongs with sigR[2] and sigS[2].

    FiatContractV1 contract = new FiatContractV1();
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
    sizeOfPreviousArrays += sigS.size() + 1;

    // Recipient Data
    response += serializeStringList(recipients);

    // Amount Data
    response += serializeLongList(amount);

    // SigV Data
    response += serializeStringList(sigV);

    // SigR Data
    response += serializeStringList(sigR);

    // SigS Data
    response += serializeStringList(sigS);

    return response;
  }

  private String calculatePointer(long numberOfParams, long sizeOfPreviousArrays) {
    return ByteUtilities.toHexString(
        BigInteger.valueOf(32 * (numberOfParams + sizeOfPreviousArrays)).toByteArray());
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

  private String serializeLongList(List<Long> data) {
    String response = "";

    String formattedString =
        ByteUtilities.toHexString(BigInteger.valueOf(data.size()).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    for (Long dataItem : data) {
      formattedString = ByteUtilities.toHexString(BigInteger.valueOf(dataItem).toByteArray());
      formattedString = String.format("%64s", formattedString).replace(' ', '0');
      response += formattedString;
    }

    return response;
  }

  @Override
  public Map<String, List<String>> parseTransfer(String bytecode) {
    FiatContractV1 contract = new FiatContractV1();
    int bufPointer = 0;
    final int paramSize = 64;
    String readValue = "";

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
    BigInteger intValue = BigInteger.ZERO;
    intValue = new BigInteger("00" + readValue, 16);
    HashMap<String, List<String>> parameters = new HashMap<>();
    parameters.put(NONCE, Arrays.asList(intValue.toString(10)));

    // Sender
    readValue = bytecode.substring(bufPointer, bufPointer + paramSize);
    bufPointer += paramSize;
    readValue = ByteUtilities
        .toHexString(ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(readValue)));
    parameters.put(SENDER, Arrays.asList(readValue));

    // Recipient
    parameters.put(RECIPIENTS, parseArray(bufPointer, bytecode, false));
    bufPointer += paramSize;

    // Amount
    parameters.put(AMOUNT, parseArray(bufPointer, bytecode, true));
    bufPointer += paramSize;

    // SigV
    parameters.put(SIGV, parseArray(bufPointer, bytecode, false));
    bufPointer += paramSize;

    // SigR
    parameters.put(SIGR, parseArray(bufPointer, bytecode, false));
    bufPointer += paramSize;

    // SigS
    parameters.put(SIGS, parseArray(bufPointer, bytecode, false));
    bufPointer += paramSize;

    return parameters;
  }

  private List<String> parseArray(int bufPointer, String buffer, boolean decodeToInt) {
    final int paramSize = 64;
    // Find the location of the array data
    String readValue = buffer.substring(bufPointer, bufPointer + paramSize);
    BigInteger intValue = new BigInteger("00" + readValue, 16);
    int arrayPointer = intValue.intValue() * 2; // String vs Bytes
    arrayPointer +=
        new FiatContractV1().getTransfer().length(); // Offset by function id, not included in calc.

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
      readValue = ByteUtilities
          .toHexString(ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(readValue)));

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
