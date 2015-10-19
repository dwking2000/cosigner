package io.emax.heimdal.ethereum.common;

public interface RLPEntity {
  public byte[] getEncodedContents();

  public void setEncodedContents(byte[] input);

  public byte[] getDecodedContents();

  public void setDecodedContents(byte[] decodedContents);

  public byte[] encode();
}
