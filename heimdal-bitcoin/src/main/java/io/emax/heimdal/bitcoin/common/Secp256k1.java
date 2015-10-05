package io.emax.heimdal.bitcoin.common;

import java.math.BigInteger;
import java.security.Security;

import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

// TODO BTC uses DER encoding, so need something to implement that.

public class Secp256k1 {
  public static byte[] getPublicKey(byte[] privateKey) {
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

  // TODO - Encode R & S to DER, BTC doesn't need the recovery ID
  public static byte[] signTransaction(byte[] data, byte[] privateKey) {
    try {
      Security.addProvider(new BouncyCastleProvider());
      ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");

      ECDSASigner ecdsaSigner = new ECDSASigner();
      ECDomainParameters domain = new ECDomainParameters(spec.getCurve(), spec.getG(), spec.getN());
      ECPrivateKeyParameters privateKeyParms =
          new ECPrivateKeyParameters(new BigInteger(1, privateKey), domain);
      ParametersWithRandom params = new ParametersWithRandom(privateKeyParms);

      ecdsaSigner.init(true, params);

      BigInteger[] sig = ecdsaSigner.generateSignature(data);
      String sigData = "00";
      sigData += ByteUtilities.toHexString(new byte[] {});
      for (BigInteger sigChunk : sig) {
        sigData += sigChunk.toString(16);
      }
      return new BigInteger(sigData, 16).toByteArray();

    } catch (Exception e) {
      System.out.println("Panic!!" + e.toString());
      e.printStackTrace(System.out);
      return null;
    }
  }
}
