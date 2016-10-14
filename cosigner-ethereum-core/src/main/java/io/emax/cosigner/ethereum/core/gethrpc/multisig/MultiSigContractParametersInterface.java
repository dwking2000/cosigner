package io.emax.cosigner.ethereum.core.gethrpc.multisig;

import java.math.BigInteger;
import java.util.List;

public interface MultiSigContractParametersInterface {

  // Getters & Setters
  List<String> getAddress();

  void setAddress(List<String> address);

  List<BigInteger> getValue();

  void setValue(List<BigInteger> value);

  BigInteger getNonce();

  void setNonce(BigInteger nonce);

  List<BigInteger> getSigV();

  void setSigV(List<BigInteger> sigV);

  List<BigInteger> getSigR();

  void setSigR(List<BigInteger> sigR);

  List<BigInteger> getSigS();

  void setSigS(List<BigInteger> sigS);

  String getFunction();

  void setFunction(String function);

  /**
   * Encode the parameters into the byte array that the contract is expecting.
   *
   * @return Byte array encoding of the parameters.
   */
  byte[] encode();

  /**
   * Decode a byte array of parameters into this data structure for easier manipulation.
   *
   * @param data Byte array representation of the parameters
   * @return Data structure of parameters.
   */
  MultiSigContractParametersInterface decode(byte[] data);

}
