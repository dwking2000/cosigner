package io.emax.cosigner.ethereum.common;

import java.math.BigInteger;
import java.security.Security;

import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECAlgorithms;
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
      return null;
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

      BigInteger[] sig = ecdsaSigner.generateSignature(data);
      StringBuilder sigData = new StringBuilder();
      sigData.append("00");
      byte[] publicKey = getPublicKey(privateKey);
      byte recoveryId = getRecoveryId(sig[0].toByteArray(), sig[1].toByteArray(), data, publicKey);
      sigData.append(ByteUtilities.toHexString(new byte[] {recoveryId}));
      for (BigInteger sigChunk : sig) {
        sigData.append(sigChunk.toString(16));
      }
      return new BigInteger(sigData.toString(), 16).toByteArray();

    } catch (Exception e) {
      System.out.println("Panic!!" + e.toString());
      e.printStackTrace(System.out);
      return null;
    }
  }

  public static byte getRecoveryId(byte[] r, byte[] s, byte[] message, byte[] publicKey) {
    ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
    BigInteger N = spec.getN();
    for (int recoveryId = 0; recoveryId < 2; recoveryId++) {
      try {
        BigInteger x = new BigInteger(1, r);

        X9IntegerConverter x9 = new X9IntegerConverter();
        byte[] compEnc = x9.integerToBytes(x, 1 + x9.getByteLength(spec.getCurve()));
        compEnc[0] = (byte) ((recoveryId & 1) == 1 ? 0x03 : 0x02);
        ECPoint R = spec.getCurve().decodePoint(compEnc);
        if (!R.multiply(N).isInfinity()) {
          continue;
        }

        BigInteger e = new BigInteger(1, message);
        BigInteger eInv = BigInteger.ZERO.subtract(e).mod(N);
        BigInteger rInv = new BigInteger(1, r).modInverse(N);
        BigInteger srInv = rInv.multiply(new BigInteger(1, s)).mod(N);
        BigInteger eInvrInv = rInv.multiply(eInv).mod(N);
        ECPoint q = ECAlgorithms.sumOfTwoMultiplies(spec.getG(), eInvrInv, R, srInv);
        byte[] qBytes = q.getEncoded(false);
        boolean matchedKeys = true;
        for (int j = 0; j < publicKey.length; j++) {
          if (qBytes[j] != publicKey[j]) {
            matchedKeys = false;
            break;
          }
        }
        if (!matchedKeys) {
          continue;
        }
        return (byte) (0xFF & (27 + recoveryId));
      } catch (Exception e) {
        e.printStackTrace();
        continue;
      }
    }

    return (byte) 0x00;
  }

  public static byte[] recoverPublicKey(byte[] r, byte[] s, byte[] message, int recoveryId) {
    ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
    BigInteger N = spec.getN();

    try {
      BigInteger x = new BigInteger(1, r);

      X9IntegerConverter x9 = new X9IntegerConverter();
      byte[] compEnc = x9.integerToBytes(x, 1 + x9.getByteLength(spec.getCurve()));
      compEnc[0] = (byte) ((recoveryId & 1) == 1 ? 0x03 : 0x02);
      ECPoint R = spec.getCurve().decodePoint(compEnc);
      if (!R.multiply(N).isInfinity()) {
        return null;
      }

      BigInteger e = new BigInteger(1, message);
      BigInteger eInv = BigInteger.ZERO.subtract(e).mod(N);
      BigInteger rInv = new BigInteger(1, r).modInverse(N);
      BigInteger srInv = rInv.multiply(new BigInteger(1, s)).mod(N);
      BigInteger eInvrInv = rInv.multiply(eInv).mod(N);
      ECPoint q = ECAlgorithms.sumOfTwoMultiplies(spec.getG(), eInvrInv, R, srInv);
      byte[] qBytes = q.getEncoded(false);
      return qBytes;
    } catch (Exception e) {
      e.printStackTrace();

    }

    return null;
  }
}
