package io.emax.cosigner.bitcoin.common;

import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.Security;

public class Secp256k1 {
  /**
   * Converts a private key into its corresponding public key.
   * 
   * @param privateKey ECDSA private key.
   * @return Public key.
   */
  public static byte[] getPublicKey(byte[] privateKey) {
    try {
      ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
      ECPoint pointQ = spec.getG().multiply(new BigInteger(1, privateKey));

      return pointQ.getEncoded(false);
    } catch (Exception e) {
      System.out.println("Panic!!" + e.toString());
      e.printStackTrace(System.out);
      return new byte[0];
    }
  }

  /** 
   * Signs a message using the provided private key.
   * 
   * @param data Message to sign. This is expected to already be hashed.
   * @param privateKey Private key to sign the data with.
   * @return ECDSA signature data, DER encoded.
   */
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
