package io.emax.cosigner.bitcoin.common;

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

public class Secp256k1 {
  public static byte[] getPublicKey(byte[] privateKey) {
    try {
      ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
      ECPoint Q = spec.getG().multiply(new BigInteger(1, privateKey));

      return Q.getEncoded(false);
    } catch (Exception e) {
      System.out.println("Panic!!" + e.toString());
      e.printStackTrace(System.out);
      return new byte[0];
    }
  }

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

      StringBuilder signature = new StringBuilder();
      BigInteger[] sig = ecdsaSigner.generateSignature(data);
      if (sig.length > 1) {
        // Nothing went wrong, we have an S, check it.
        final BigInteger maxS = new BigInteger(1, ByteUtilities
            .toByteArray("7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF5D576E7357A4501DDFE92F46681B20A0"));

        if (sig[1].compareTo(maxS) == 1) {
          // S is too big, sign again (BIP62)
          return signTransaction(data, privateKey);
        }
      }
      for (BigInteger sigData : sig) {
        signature.append("02");
        byte[] sigBytes = sigData.toByteArray();
        byte[] sigSize = BigInteger.valueOf(sigBytes.length).toByteArray();
        sigSize = ByteUtilities.stripLeadingNullBytes(sigSize);
        signature.append(ByteUtilities.toHexString(sigSize));
        signature.append(ByteUtilities.toHexString(sigBytes));
      }

      byte[] sigBytes = ByteUtilities.toByteArray(signature.toString());
      byte[] sigSize = BigInteger.valueOf(sigBytes.length).toByteArray();
      sigSize = ByteUtilities.stripLeadingNullBytes(sigSize);
      String signatureString = ByteUtilities.toHexString(sigSize) + signature.toString();
      signatureString = "30" + signatureString;

      return ByteUtilities.toByteArray(signatureString);

    } catch (Exception e) {
      System.out.println("Panic!!" + e.toString());
      e.printStackTrace(System.out);
      return new byte[0];
    }
  }
}
