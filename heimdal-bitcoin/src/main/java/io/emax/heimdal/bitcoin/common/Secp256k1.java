package io.emax.heimdal.bitcoin.common;

import java.math.BigInteger;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

public class Secp256k1 {
  public byte[] getPublicKey(byte[] privateKey) {
    try {
      ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
      ECPoint Q = spec.getG().multiply(new BigInteger(1, privateKey));

      return Q.getEncoded(false);
    } catch (Exception e) {
      System.out.println("Panic!!" + e.toString());
      e.printStackTrace(System.out);
      return null;
    }
  }
}
