package io.emax.heimdal.ethereum.common;

import java.math.BigInteger;

import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECAlgorithms;
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
  
  public byte[] signTransaction(byte[] data, byte[] privateKey){
    try {
      ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");

      ECDSASigner ecdsaSigner = new ECDSASigner();      
      ECDomainParameters domain = new ECDomainParameters(spec.getCurve(), spec.getG(), spec.getN());
      ECPrivateKeyParameters privateKeyParms = new ECPrivateKeyParameters(new BigInteger(1, privateKey), domain);
      ParametersWithRandom params = new ParametersWithRandom(privateKeyParms); // TODO Add random parameter here.
      ecdsaSigner.init(true, params);

      BigInteger[] sig = ecdsaSigner.generateSignature(data);
      String sigData = "00";
      byte[] publicKey = getPublicKey(privateKey);
      byte recoveryId = getRecoveryId(sig[0].toByteArray(), sig[1].toByteArray(), data, publicKey);
      sigData += DeterministicTools.toHex(new byte[] {recoveryId});
      for(BigInteger sigChunk : sig){
        sigData += sigChunk.toString(16);
      }
      return new BigInteger(sigData, 16).toByteArray();

    } catch (Exception e) {
      System.out.println("Panic!!" + e.toString());
      e.printStackTrace(System.out);
      return null;
    }
  }
  
  public byte getRecoveryId(byte[] r, byte[] s, byte[] message, byte[] publicKey){
    ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
    BigInteger N = spec.getN();
    for(int recoveryId = 0; recoveryId < 2; recoveryId++) {
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
      ECPoint.Fp q = (ECPoint.Fp) ECAlgorithms.sumOfTwoMultiplies(spec.getG(), eInvrInv, R, srInv);
      byte[] qBytes = q.getEncoded(false); 
      for(int j = 0; j < publicKey.length; j++){
        if(qBytes[j] != publicKey[j]) {
          continue;
        }
      }
      return (byte)(0xFF & recoveryId);
    }
    
    return (byte)0x00;
    
  }
}
