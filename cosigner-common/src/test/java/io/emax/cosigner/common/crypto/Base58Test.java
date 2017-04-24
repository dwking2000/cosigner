package io.emax.cosigner.common.crypto;

import io.emax.cosigner.common.Base58;

import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

public class Base58Test {
  @Test
  public void testBase58() throws UnsupportedEncodingException {
    String phrase = "TESTPHRASE";
    BigInteger intTest = BigInteger.valueOf(1552);

    String strResult = Base58.encode(phrase.getBytes("UTF-8"));
    try {
      strResult = new String(Base58.decode(strResult), "UTF-8");
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("Error decoding phrase.");
    }
    Assert.assertTrue("Phrases do not match.", strResult.equalsIgnoreCase(phrase));

    strResult = Base58.encode(intTest.toByteArray());
    BigInteger intResult = BigInteger.ZERO;
    try {
      intResult = Base58.decodeToBigInteger(strResult);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("Error decoding integer.");
    }
    Assert.assertTrue("Integers do not match", intTest.compareTo(intResult) == 0);
  }
}
