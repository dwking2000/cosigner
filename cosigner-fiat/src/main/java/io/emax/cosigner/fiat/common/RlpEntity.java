package io.emax.cosigner.fiat.common;

public interface RlpEntity {
  byte[] getEncodedContents();

  void setEncodedContents(byte[] input);

  byte[] getDecodedContents();

  void setDecodedContents(byte[] decodedContents);

  byte[] encode();
}
