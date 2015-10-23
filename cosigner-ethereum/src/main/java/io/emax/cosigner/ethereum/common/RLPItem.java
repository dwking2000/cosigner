package io.emax.cosigner.ethereum.common;

import java.io.Serializable;
import java.util.Arrays;

public class RLPItem implements RLPEntity, Serializable {
  private static final long serialVersionUID = 1L;
  private byte[] encodedContents;
  private byte[] decodedContents;

  public RLPItem(byte[] decodedBytes) {
    this.setDecodedContents(decodedBytes);
  }

  public RLPItem() {
    this.setDecodedContents(new byte[] {});
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
    return "RLPItem [encodedContents=" + Arrays.toString(encodedContents) + ", decodedContents="
        + Arrays.toString(decodedContents) + "]";
  }

  @Override
  public byte[] encode() {
    encodedContents = RLP.encodeItem(decodedContents);
    byte[] retArray = new byte[encodedContents.length];
    System.arraycopy(encodedContents, 0, retArray, 0, encodedContents.length);
    return retArray;
  }
}
