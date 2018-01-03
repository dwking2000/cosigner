package io.emax.cosigner.ethereum.core.common;

import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.DeterministicRng;
import io.emax.cosigner.common.crypto.Secp256k1;
import io.emax.cosigner.ethereum.core.gethrpc.CallData;

import org.apache.commons.lang3.StringUtils;
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

    byte[] userKey = new BigInteger(userKeyPart.isEmpty() ? "00" : userKeyPart, 16).toByteArray();
    byte[] serverKey =
        new BigInteger(serverKeyPart.isEmpty() ? "00" : serverKeyPart, 16).toByteArray();
    SecureRandom secureRandom = DeterministicRng.getSecureRandom(userKey, serverKey);

    // Set up our private key variables
    BigInteger privateKeyCheck = BigInteger.ZERO;

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

      byte[] publicShaKeyBytes = ByteUtilities
          .toByteArray(EthereumTools.hashKeccak(ByteUtilities.toHexString(publicKeyBytes)));

      LOGGER.debug("Got address hash: " + ByteUtilities.toHexString(publicShaKeyBytes));
      byte[] decodedPublicKey = Arrays.copyOfRange(publicShaKeyBytes, 96 / 8, 256 / 8);
      BigInteger publicKey = new BigInteger(1, decodedPublicKey);
      String address = publicKey.toString(16);
      address = StringUtils.leftPad(address, 40, "0");
      return address;

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

      byte[] publicKey = Secp256k1.getPublicKey(decodedPrivateKey);
      String croppedKey = ByteUtilities.toHexString(publicKey);
      croppedKey = croppedKey.substring(2);
      LOGGER.debug("Private key to public conversion: " + croppedKey);
      return ByteUtilities.toByteArray(croppedKey);

    } catch (Exception e) {
      LOGGER.warn(null, e);
      return new byte[0];
    }
  }

  public static String calculateContractAddress(String creator, Long txNumber) {
    RlpList contractAddress = new RlpList();
    RlpItem contractCreator = new RlpItem(ByteUtilities.toByteArray(creator));
    RlpItem nonce = new RlpItem(
        ByteUtilities.stripLeadingNullBytes(BigInteger.valueOf(txNumber).toByteArray()));
    contractAddress.add(contractCreator);
    contractAddress.add(nonce);

    return EthereumTools.hashKeccak(ByteUtilities.toHexString(contractAddress.encode()))
        .substring(96 / 4, 256 / 4);
  }

  public static CallData generateCall(String data, String address) {
    CallData callData = new CallData();
    callData.setTo("0x" + address);
    callData.setValue("0x0");
    callData.setData("0x" + data);
    callData.setGas("0x186A0");
    callData.setGasPrice("0x186A0");
    return callData;
  }
}
