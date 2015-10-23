package io.emax.cosigner.ethereum.common;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.crypto.digests.SHA3Digest;

public class DeterministicTools {

  private static final String RANDOM_NUMBER_ALGORITHM = "SHA1PRNG";
  private static final String RANDOM_NUMBER_ALGORITHM_PROVIDER = "SUN";

  /**
   * 
   * @param userKeyPart Expect these to be hex strings without the leading 0x identifier
   * @param serverKeyPart Expect these to be hex strings without the leading 0x identifier
   * @param rounds
   * @return
   */
  public static String getDeterministicPrivateKey(String userKeyPart, String serverKeyPart,
      int rounds) {
    SecureRandom secureRandom;
    try {
      secureRandom =
          SecureRandom.getInstance(RANDOM_NUMBER_ALGORITHM, RANDOM_NUMBER_ALGORITHM_PROVIDER);
    } catch (Exception e) {
      e.printStackTrace();
      secureRandom = new SecureRandom();
    }

    byte[] userKey = new BigInteger(userKeyPart, 16).toByteArray();
    byte[] serverKey = new BigInteger(serverKeyPart, 16).toByteArray();
    byte[] userSeed = new byte[Math.max(userKey.length, serverKey.length)];

    // XOR the key parts to get our seed, repeating them if they lengths
    // don't match
    for (int i = 0; i < userSeed.length; i++) {
      userSeed[i] = (byte) (userKey[i % userKey.length] ^ serverKey[i % serverKey.length]);
    }

    // Set up out private key variables
    BigInteger privateKeyCheck = BigInteger.ZERO;
    secureRandom.setSeed(userSeed);
    // Bit of magic, move this maybe. This is the max key range.
    BigInteger maxKey =
        new BigInteger("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364140", 16);

    // Generate the key, skipping as many as desired.
    byte[] privateKeyAttempt = new byte[32];
    for (int i = 0; i < Math.max(rounds, 1); i++) {
      secureRandom.nextBytes(privateKeyAttempt);
      privateKeyCheck = new BigInteger(1, privateKeyAttempt);
      while (privateKeyCheck.compareTo(BigInteger.ZERO) == 0
          || privateKeyCheck.compareTo(maxKey) == 1) {
        secureRandom.nextBytes(privateKeyAttempt);
        privateKeyCheck = new BigInteger(1, privateKeyAttempt);
      }
    }

    return privateKeyCheck.toString(16);
  }

  public static String hashSha3(String data) {
    byte[] dataBytes = ByteUtilities.toByteArray(data);
    SHA3Digest md = new SHA3Digest(256);
    md.reset();
    md.update(dataBytes, 0, dataBytes.length);
    byte[] hashedBytes = new byte[256 / 8];
    md.doFinal(hashedBytes, 0);
    return ByteUtilities.toHexString(hashedBytes);
  }

  public static String encodeUserKey(String key) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(key.getBytes("UTF-8"));
      return new BigInteger(md.digest()).toString(16);
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] getRandomBytes(int size) {
    SecureRandom secureRandom;
    try {
      secureRandom =
          SecureRandom.getInstance(RANDOM_NUMBER_ALGORITHM, RANDOM_NUMBER_ALGORITHM_PROVIDER);
    } catch (Exception e) {
      e.printStackTrace();
      secureRandom = new SecureRandom();
    }

    byte[] randBytes = new byte[size];
    secureRandom.nextBytes(randBytes);

    return randBytes;
  }

  public static String getPublicAddress(String privateKey) {
    return getPublicAddress(privateKey, true);
  }

  public static String getPublicAddress(String key, boolean isPrivateKey) {
    try {
      byte[] publicKeyBytes;
      if (isPrivateKey) {
        publicKeyBytes = getPublicKeyBytes(key);
      } else {
        publicKeyBytes = ByteUtilities.toByteArray(key);
      }

      SHA3Digest md = new SHA3Digest(256);
      md.reset();
      md.update(publicKeyBytes, 1, publicKeyBytes.length - 1);
      byte[] publicShaKeyBytes = new byte[32];
      md.doFinal(publicShaKeyBytes, 0);

      byte[] decodedPublicKey = Arrays.copyOfRange(publicShaKeyBytes, 96 / 8, 256 / 8);
      BigInteger publicKey = new BigInteger(1, decodedPublicKey);
      return publicKey.toString(16);

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static String getPublicKey(String privateKey) {
    return ByteUtilities.toHexString(getPublicKeyBytes(privateKey));
  }

  public static byte[] getPublicKeyBytes(String privateKey) {
    try {
      byte[] decodedPrivateKey = new BigInteger("00" + privateKey, 16).toByteArray();

      byte[] publicKeyBytes = Secp256k1.getPublicKey(decodedPrivateKey);

      return publicKeyBytes;

    } catch (Exception e) {
      e.printStackTrace();
      return new byte[0];
    }
  }
}
