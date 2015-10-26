package io.emax.cosigner.ethereum.common;

import java.util.Arrays;
import java.util.Locale;

public class ByteUtilities {

  /**
   * String leading 0x00 bytes from a byte array.
   * 
   * @param input Any byte array
   * @return New array with all leading 0x00's stripped from the data.
   */
  public static byte[] stripLeadingNullBytes(byte[] input) {
    byte[] result = Arrays.copyOf(input, input.length);
    while (result.length > 0 && result[0] == 0x00) {
      result = Arrays.copyOfRange(result, 1, result.length);
    }
    return result;
  }

  static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

  /**
   * Convert a byte array to string form.
   * 
   * @param data Any byte array
   * @return A string representation of the array.
   */
  public static String toHexString(byte[] data) {
    char[] chars = new char[data.length * 2];
    for (int i = 0; i < data.length; i++) {
      chars[i * 2] = HEX_DIGITS[(data[i] >> 4) & 0xf];
      chars[i * 2 + 1] = HEX_DIGITS[data[i] & 0xf];
    }
    return new String(chars).toLowerCase(Locale.US);
  }

  /**
   * Converts a string representation of a byte array to an actual byte array.
   * 
   * @param data String representing a byte array
   * @return Byte array
   */
  public static byte[] toByteArray(String data) {
    if (data == null) {
      return new byte[] {};
    }

    if (data.substring(0, 2).toLowerCase(Locale.US).equals("0x")) {
      data = data.substring(2);
    }
    if (data.length() % 2 != 0) {
      data = "0" + data;
    }

    data = data.toUpperCase(Locale.US);

    byte[] bytes = new byte[data.length() / 2];
    String hexString = new String(HEX_DIGITS);
    for (int i = 0; i < bytes.length; i++) {
      int byteConv = hexString.indexOf(data.charAt(i * 2)) * 0x10;
      byteConv += hexString.indexOf(data.charAt(i * 2 + 1));
      bytes[i] = (byte) (byteConv & 0xFF);
    }

    return bytes;
  }
}
