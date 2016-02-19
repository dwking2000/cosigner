package io.emax.cosigner.fiat.gethrpc.FiatContract.v1;

import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.fiat.gethrpc.FiatContract.FiatContractParametersInterface;

import java.math.BigInteger;
import java.util.List;

public class FiatContractParametersV1 implements FiatContractParametersInterface {
  // TODO For each function, create a method that takes arguments and converts them to bytecode
  // TODO Create a method that takes bytecode for a transfer and converts it back to arguments

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

  public String createTokens(long numTokens) {
    FiatContractV1 contract = new FiatContractV1();
    String response = contract.getCreateTokens();

    String formattedString = ByteUtilities.toHexString(BigInteger.valueOf(numTokens).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    return response;
  }

  public String destroyTokens(long numTokens) {
    FiatContractV1 contract = new FiatContractV1();
    String response = contract.getDestroyTokens();

    String formattedString = ByteUtilities.toHexString(BigInteger.valueOf(numTokens).toByteArray());
    formattedString = String.format("%64s", formattedString).replace(' ', '0');
    response += formattedString;

    return response;
  }

  public String getOwners() {
    FiatContractV1 contract = new FiatContractV1();
    String response = contract.getGetOwners();

    return response;
  }

  public String getBalance(String address) {
    FiatContractV1 contract = new FiatContractV1();
    String response = contract.getGetBalance();

    String formattedString = String.format("%64s", address).replace(' ', '0');
    response += formattedString;

    return response;
  }

  public String getTotalBalance() {
    FiatContractV1 contract = new FiatContractV1();
    String response = contract.getGetTotalBalance();

    return response;
  }

  public String isOwner(String address) {
    FiatContractV1 contract = new FiatContractV1();
    String response = contract.getIsOwner();

    String formattedString = String.format("%64s", address).replace(' ', '0');
    response += formattedString;

    return response;
  }

  // TODO Transfer
  // TODO Parse Transfer Bytecode
}
