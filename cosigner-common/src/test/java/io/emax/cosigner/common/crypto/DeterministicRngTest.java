package io.emax.cosigner.common.crypto;

import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.DeterministicRng;

import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

public class DeterministicRngTest {
  @Test
  public void testRNG() {
    String userKey = "deadbeefdeadbeefdeadbeef";
    String serverKey = "1234deadbeef1234";
    String expectedResult = "077b2c46a5e76273c67a8eda1d24d631cae28adcb654f025f59b1b19ab4e36c0";

    SecureRandom myRng = DeterministicRng
        .getSecureRandom(ByteUtilities.toByteArray(userKey), ByteUtilities.toByteArray(serverKey));

    byte[] testBytes = new byte[32];
    myRng.nextBytes(testBytes);
    String testResult = ByteUtilities.toHexString(testBytes);
    Assert.assertTrue("RNG results are not matching", testResult.equalsIgnoreCase(expectedResult));
  }
}
