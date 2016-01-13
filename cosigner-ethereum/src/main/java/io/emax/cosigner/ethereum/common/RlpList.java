package io.emax.cosigner.ethereum.common;

import io.emax.cosigner.common.ByteUtilities;

import org.bouncycastle.util.Arrays;

import java.util.LinkedList;

public class RlpList extends LinkedList<RlpEntity> implements RlpEntity {
  private static final long serialVersionUID = 1L;
  private byte[] encodedContents;
  private byte[] decodedContents;

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
    int bufferPointer = 0;
    while (bufferPointer < this.decodedContents.length) {
      RlpEntity newEntity = Rlp.parseArray(
          Arrays.copyOfRange(this.decodedContents, bufferPointer, this.decodedContents.length));
      if (newEntity != null) {
        this.add(newEntity);
        bufferPointer = bufferPointer + newEntity.getEncodedContents().length;
      } else {
        return;
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    output.append("RLPList [encodedContents=").append(ByteUtilities.toHexString(encodedContents))
        .append(", decodedContents=").append(ByteUtilities.toHexString(decodedContents));

    for (RlpEntity entity : this) {
      output.append("\n {").append(entity.toString()).append("}");
    }

    output.append("]");
    return output.toString();
  }

  @Override
  public byte[] encode() {
    encodedContents = Rlp.encodeList(this);
    byte[] retArray = new byte[encodedContents.length];
    System.arraycopy(encodedContents, 0, retArray, 0, encodedContents.length);
    return retArray;
  }
}
