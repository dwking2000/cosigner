package io.emax.heimdal.blockchains.ethereum;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

public class TestUtils {

  public static boolean isHexNumber(String cadena) {
    // Check if string is hexadecimal
    // http://stackoverflow.com/questions/11424540/verify-if-string-is-hexadecimal
    try {
      // noinspection ResultOfMethodCallIgnored
      new BigInteger(cadena.replaceAll("^0x", ""), 16);
      return true;
    } catch (NumberFormatException ex) {
      // Error handling code...
      return false;
    }
  }

  public static File createTempDirectory() throws IOException {
    final File temp;

    temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

    if (!(temp.delete())) {
      throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
    }

    if (!(temp.mkdir())) {
      throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
    }

    temp.deleteOnExit();
    return temp;
  }
}
