package io.emax.cosigner.common.crypto;

import io.emax.cosigner.common.ByteUtilities;

import org.junit.Assert;
import org.junit.Test;

public class ByteUtilityTest {
  @Test
  public void testLeftPadAndStrip() {
    String base = "deadbeef";
    String padded = "00000000deadbeef";

    byte[] testBytes = ByteUtilities.toByteArray(base);
    testBytes = ByteUtilities.leftPad(testBytes, 8, (byte) 0x00);
    String testResults = ByteUtilities.toHexString(testBytes);
    Assert.assertTrue("Padded arrays do not match", padded.equalsIgnoreCase(testResults));

    testBytes = ByteUtilities.stripLeadingNullBytes(testBytes);
    testResults = ByteUtilities.toHexString(testBytes);
    Assert.assertTrue("Stripped arrays do not match", base.equalsIgnoreCase(testResults));
  }

  @Test
  public void testFlipEndian() {
    String base = "deadbeef";
    String flipped = "efbeadde";

    byte[] testBytes = ByteUtilities.toByteArray(base);
    testBytes = ByteUtilities.flipEndian(testBytes);
    String testResult = ByteUtilities.toHexString(testBytes);
    Assert.assertTrue("Flipped arrays do not match", testResult.equalsIgnoreCase(flipped));
  }
}
