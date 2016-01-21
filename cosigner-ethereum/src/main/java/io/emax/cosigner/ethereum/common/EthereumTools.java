package io.emax.cosigner.ethereum.common;

import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.crypto.Secp256k1;

import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class EthereumTools {
  private static final Logger LOGGER = LoggerFactory.getLogger(EthereumTools.class);
  private static final String RANDOM_NUMBER_ALGORITHM = "SHA1PRNG";
  private static final String RANDOM_NUMBER_ALGORITHM_PROVIDER = "SUN";

  /**
   * Generates a deterministic random number that can be used as a private key in ECDSA.
   *
   * @param userKeyPart   Expect these to be hex strings without the leading 0x identifier. This
   *                      combined with the serverKeyPart is used as a seed to generate the private
   *                      keys.
   * @param serverKeyPart Expect these to be hex strings without the leading 0x identifier. This
   *                      combined with the userKeyPart is used as a seed to generate the private
   *                      keys.
   * @param rounds        Number of private keys to skip while generating them.
   * @return A private key encoded in a hex string.
   */
  public static String getDeterministicPrivateKey(String userKeyPart, String serverKeyPart,
      int rounds) {
    SecureRandom secureRandom;
    try {
      secureRandom =
          SecureRandom.getInstance(RANDOM_NUMBER_ALGORITHM, RANDOM_NUMBER_ALGORITHM_PROVIDER);
    } catch (Exception e) {
      LOGGER.error(null, e);
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

  /**
   * Generates a SHA-3 hash on the provided data.
   *
   * @param data Hex data encoded in a string.
   * @return Hash encoded in a hex string.
   */
  public static String hashKeccak(String data) {
    //public static String hashKeccak(String data) {
    byte[] dataBytes = ByteUtilities.toByteArray(data);
    Keccak.DigestKeccak md = new Keccak.DigestKeccak(256);
    md.reset();
    md.update(dataBytes, 0, dataBytes.length);
    byte[] hashedBytes = md.digest();
    return ByteUtilities.toHexString(hashedBytes);
  }

  /**
   * Obscures the userKey which is sensitive data.
   *
   * <p>Intended to be used when the key needs to be identified but also exported, to the geth node
   * for example.
   *
   * @param key User secret that needs to be hidden
   * @return Hashed user key
   */
  public static String encodeUserKey(String key) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(key.getBytes("UTF-8"));
      return new BigInteger(md.digest()).toString(16);
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      LOGGER.error(null, e);
      return null;
    }
  }

  public static String getPublicAddress(String privateKey) {
    return getPublicAddress(privateKey, true);
  }

  /**
   * Converts a private key, or public key, to the public address used in Ethereum.
   *
   * @param key          Key being converted.
   * @param isPrivateKey Do we need to convert from a private key to public, or is it already a
   *                     public key.
   * @return The public address that people using Ethereum are used to seeing.
   */
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
      LOGGER.error(null, e);
      return null;
    }
  }

  public static String getPublicKey(String privateKey) {
    return ByteUtilities.toHexString(getPublicKeyBytes(privateKey));
  }

  /**
   * Convert a private key into the corresponding public one.
   *
   * @param privateKey Private key being converted.
   * @return Public key that corresponds to the input.
   */
  public static byte[] getPublicKeyBytes(String privateKey) {
    try {
      byte[] decodedPrivateKey = new BigInteger("00" + privateKey, 16).toByteArray();

      return Secp256k1.getPublicKey(decodedPrivateKey);

    } catch (Exception e) {
      LOGGER.warn(null, e);
      return new byte[0];
    }
  }
}
