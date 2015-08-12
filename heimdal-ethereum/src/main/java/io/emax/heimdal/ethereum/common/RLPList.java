package io.emax.heimdal.ethereum.common;

import java.util.LinkedList;

import org.bouncycastle.util.Arrays;

public class RLPList extends LinkedList<RLPEntity> implements RLPEntity {
  private static final long serialVersionUID = 1L;
  private byte[] encodedContents;
  private byte[] decodedContents;

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
    int bufferPointer = 0;
    while (bufferPointer < this.decodedContents.length) {
      RLPEntity newEntity = RLP.parseArray(Arrays.copyOfRange(this.decodedContents, bufferPointer, this.decodedContents.length));
      if(newEntity != null) {
        this.add(newEntity);
        bufferPointer = bufferPointer + newEntity.getEncodedContents().length;
      } else {
        return;
      }
    }
  }

  @Override
  public String toString() {
    String output = "RLPList [encodedContents=" + java.util.Arrays.toString(encodedContents)
        + ", decodedContents=" + java.util.Arrays.toString(decodedContents);
    
    for(RLPEntity entity : this) {
      output += "\n {" + entity.toString() + "}";
    }
    
    output += "]";
    return output;
  }

  @Override
  public byte[] encode() {
    this.encodedContents = RLP.encodeList(this);
    return this.encodedContents;
  }
}
