package io.emax.cosigner.fiat.common;

import io.emax.cosigner.common.ByteUtilities;

import java.io.Serializable;

public class RlpItem implements RlpEntity, Serializable {
  private static final long serialVersionUID = 1L;
  private byte[] encodedContents;
  private byte[] decodedContents;

  public RlpItem(byte[] decodedBytes) {
    this.setDecodedContents(decodedBytes);
  }

  public RlpItem() {
    this.setDecodedContents(new byte[]{});
  }

  @Override
  public byte[] getEncodedContents() {
    byte[] retArray = new byte[encodedContents.length];
    System.arraycopy(encodedContents, 0, retArray, 0, encodedContents.length);
    return retArray;
  }

  @Override
  public void setEncodedContents(byte[] encodedContents) {
    this.encodedContents = new byte[encodedContents.length];
    System.arraycopy(encodedContents, 0, this.encodedContents, 0, encodedContents.length);
  }

  @Override
  public byte[] getDecodedContents() {
    byte[] retArray = new byte[decodedContents.length];
    System.arraycopy(decodedContents, 0, retArray, 0, decodedContents.length);
    return retArray;
  }

  @Override
  public void setDecodedContents(byte[] decodedContents) {
    this.decodedContents = new byte[decodedContents.length];
    System.arraycopy(decodedContents, 0, this.decodedContents, 0, decodedContents.length);
  }

  @Override
  public String toString() {
    return "RLPItem [encodedContents=" + ByteUtilities.toHexString(encodedContents)
        + ", decodedContents=" + ByteUtilities.toHexString(decodedContents) + "]";
  }

  @Override
  public byte[] encode() {
    encodedContents = Rlp.encodeItem(decodedContents);
    byte[] retArray = new byte[encodedContents.length];
    System.arraycopy(encodedContents, 0, retArray, 0, encodedContents.length);
    return retArray;
  }
}
