package io.emax.cosigner.ethereum.common;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.Security;

public class Secp256k1 {
  private static final Logger logger = LoggerFactory.getLogger(Secp256k1.class);

  /**
   * Convert a private key to a public key.
   * 
   * @param privateKey Private key to convert.
   * @return Corresponding public key.
   */
  public static byte[] getPublicKey(byte[] privateKey) {
    try {
      ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
      ECPoint pointQ = spec.getG().multiply(new BigInteger(1, privateKey));

      return pointQ.getEncoded(false);
    } catch (Exception e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      logger.error(errors.toString());
      return new byte[0];
    }
  }

  /**
   * Sign data using the ECDSA algorithm.
   * 
   * @param data Data that needs to be signed. This is expected to be hashed in the desired way
   *        already.
   * @param privateKey Private key to sign the data with.
   * @return Signature data in the form of sigV(1-byte)|sigR(32-bytes)|sigS(32-bytes)
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
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      logger.error(errors.toString());
      return new byte[0];
    }
  }

  /**
   * Determine the recovery ID for the given signature and public key.
   * 
   * <p>Any signed message can resolve to one of two public keys due to the nature ECDSA. The
   * recovery ID provides information about which one it is, allowing confirmation that the message
   * was signed by a specific key.</p>
   * 
   * @param sigR R value of the signature.
   * @param sigS S value of the signature.
   * @param message Data that was signed.
   * @param publicKey The public key that we expect to recover.
   * @return Recovery ID that will let us recover the expected public key.
   */
  public static byte getRecoveryId(byte[] sigR, byte[] sigS, byte[] message, byte[] publicKey) {
    ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
    BigInteger pointN = spec.getN();
    for (int recoveryId = 0; recoveryId < 2; recoveryId++) {
      try {
        BigInteger pointX = new BigInteger(1, sigR);

        X9IntegerConverter x9 = new X9IntegerConverter();
        byte[] compEnc = x9.integerToBytes(pointX, 1 + x9.getByteLength(spec.getCurve()));
        compEnc[0] = (byte) ((recoveryId & 1) == 1 ? 0x03 : 0x02);
        ECPoint pointR = spec.getCurve().decodePoint(compEnc);
        if (!pointR.multiply(pointN).isInfinity()) {
          continue;
        }

        BigInteger pointE = new BigInteger(1, message);
        BigInteger pointEInv = BigInteger.ZERO.subtract(pointE).mod(pointN);
        BigInteger pointRInv = new BigInteger(1, sigR).modInverse(pointN);
        BigInteger srInv = pointRInv.multiply(new BigInteger(1, sigS)).mod(pointN);
        BigInteger pointEInvRInv = pointRInv.multiply(pointEInv).mod(pointN);
        ECPoint pointQ = ECAlgorithms.sumOfTwoMultiplies(spec.getG(), pointEInvRInv, pointR, srInv);
        byte[] pointQBytes = pointQ.getEncoded(false);
        boolean matchedKeys = true;
        for (int j = 0; j < publicKey.length; j++) {
          if (pointQBytes[j] != publicKey[j]) {
            matchedKeys = false;
            break;
          }
        }
        if (!matchedKeys) {
          continue;
        }
        return (byte) (0xFF & (27 + recoveryId));
      } catch (Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        logger.error(errors.toString());
        continue;
      }
    }

    return (byte) 0x00;
  }

  /**
   * Recover the public key that corresponds to the private key, which signed this message.
   * 
   * @param sigR R value of the signature.
   * @param sigS S value of the signature.
   * @param message Data that was signed.
   * @param recoveryId Recovery ID provided with the signature.
   * @return The public key that corresponds to the private key, which signed the data.
   */
  public static byte[] recoverPublicKey(byte[] sigR, byte[] sigS, byte[] message, int recoveryId) {
    ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
    BigInteger pointN = spec.getN();

    try {
      BigInteger pointX = new BigInteger(1, sigR);

      X9IntegerConverter x9 = new X9IntegerConverter();
      byte[] compEnc = x9.integerToBytes(pointX, 1 + x9.getByteLength(spec.getCurve()));
      compEnc[0] = (byte) ((recoveryId & 1) == 1 ? 0x03 : 0x02);
      ECPoint pointR = spec.getCurve().decodePoint(compEnc);
      if (!pointR.multiply(pointN).isInfinity()) {
        return new byte[0];
      }

      BigInteger pointE = new BigInteger(1, message);
      BigInteger pointEInv = BigInteger.ZERO.subtract(pointE).mod(pointN);
      BigInteger pointRInv = new BigInteger(1, sigR).modInverse(pointN);
      BigInteger srInv = pointRInv.multiply(new BigInteger(1, sigS)).mod(pointN);
      BigInteger pointEInvRInv = pointRInv.multiply(pointEInv).mod(pointN);
      ECPoint pointQ = ECAlgorithms.sumOfTwoMultiplies(spec.getG(), pointEInvRInv, pointR, srInv);
      byte[] pointQBytes = pointQ.getEncoded(false);
      return pointQBytes;
    } catch (Exception e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      logger.error(errors.toString());
    }

    return new byte[0];
  }
}
