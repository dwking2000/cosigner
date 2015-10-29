package io.emax.cosigner.ethereum.gethrpc.multisig;

import java.math.BigInteger;
import java.util.LinkedList;


public interface MultiSigContractParametersInterface {

  // Getters & Setters
  LinkedList<String> getAddress();

  void setAddress(LinkedList<String> address);

  LinkedList<BigInteger> getValue();

  void setValue(LinkedList<BigInteger> value);

  BigInteger getNonce();

  void setNonce(BigInteger nonce);

  LinkedList<BigInteger> getSigV();

  void setSigV(LinkedList<BigInteger> sigV);

  LinkedList<BigInteger> getSigR();

  void setSigR(LinkedList<BigInteger> sigR);

  LinkedList<BigInteger> getSigS();

  void setSigS(LinkedList<BigInteger> sigS);

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
