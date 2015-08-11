package io.emax.heimdal.ethereum.common;

import java.util.LinkedList;

public class RLP {
  private static final int THRESHOLD = 56;
  private static final int SHORT_ITEM = 0x80;
  private static final int LONG_ITEM = 0xb7;
  private static final int SHORT_LIST = 0xc0;
  private static final int LONG_LIST = 0xf7;
  
  // Read byte, determine if it's a list, item, or byte, return as appropriate.
  public RLPEntity parseArray(byte[] input) {
    return null;
  }
  
  public byte[] encodeItem(byte[] item) {
    return null;
  }
  
  public byte[] encodeList(LinkedList<byte[]> list) {
    return null;
  }
  
  public RLPEntity decodeEntity(byte[] entity) {
    return null;
  }
}
