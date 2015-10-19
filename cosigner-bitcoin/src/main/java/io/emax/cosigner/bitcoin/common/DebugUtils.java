package io.emax.cosigner.bitcoin.common;

import java.math.BigInteger;

public class DebugUtils {
  public static String toUnsignedHexString(byte[] hex) {
    return new BigInteger(1, hex).toString(16);
  }
}
