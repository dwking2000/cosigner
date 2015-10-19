package io.emax.heimdal.ethereum.common;

import java.util.Arrays;

public class RLPItem implements RLPEntity {
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
    return encodedContents;
  }

  @Override
  public void setEncodedContents(byte[] encodedContents) {
    this.encodedContents = encodedContents;
  }

  @Override
  public byte[] getDecodedContents() {
    return decodedContents;
  }

  @Override
  public void setDecodedContents(byte[] decodedContents) {
    this.decodedContents = decodedContents;
  }

  @Override
  public String toString() {
    return "RLPItem [encodedContents=" + Arrays.toString(encodedContents) + ", decodedContents="
        + Arrays.toString(decodedContents) + "]";
  }

  @Override
  public byte[] encode() {
    this.encodedContents = RLP.encodeItem(decodedContents);
    return this.encodedContents;
  }
}
