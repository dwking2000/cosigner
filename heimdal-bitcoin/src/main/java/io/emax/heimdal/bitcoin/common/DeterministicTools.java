package io.emax.heimdal.bitcoin.common;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;

public class DeterministicTools {

  private static final String RANDOM_NUMBER_ALGORITHM = "SHA1PRNG";
  private static final String RANDOM_NUMBER_ALGORITHM_PROVIDER = "SUN";
  public static final String NOKEY = "NOKEY";

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
    } catch (Exception E) {
      secureRandom = new SecureRandom();
    }

    try {
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

      // Encode in format bitcoind is expecting
      byte[] privateKey = {(byte) 0xEF}; // TESTNET
      // byte[] privateKey = {(byte)0x80}; // REALNET
      byte[] privateKey2 = new byte[privateKey.length + privateKeyAttempt.length];
      System.arraycopy(privateKey, 0, privateKey2, 0, privateKey.length);
      System.arraycopy(privateKeyAttempt, 0, privateKey2, privateKey.length,
          privateKeyAttempt.length);
      privateKey = privateKey2.clone();

      try {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(privateKey);
        byte[] checksumHash = Arrays.copyOfRange(md.digest(md.digest()), 0, 4);

        privateKey2 = new byte[privateKey.length + checksumHash.length];
        System.arraycopy(privateKey, 0, privateKey2, 0, privateKey.length);
        System.arraycopy(checksumHash, 0, privateKey2, privateKey.length, checksumHash.length);
        privateKey = privateKey2.clone();

        return Base58.encode(privateKey);

      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
        return NOKEY;
      }
    } catch (Exception e) {
      return NOKEY;
    }
  }

  public static String encodeUserKey(String key) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(key.getBytes());
      return new BigInteger(md.digest()).toString(16);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static String getPublicAddress(String privateKey) {
    try {
      byte[] publicKeyBytes = getPublicKeyBytes(privateKey);

      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.reset();
      md.update(publicKeyBytes);
      byte[] publicShaKeyBytes = md.digest();

      RIPEMD160Digest ripemd = new RIPEMD160Digest();
      byte[] publicRipemdKeyBytes = new byte[20];
      ripemd.update(publicShaKeyBytes, 0, publicShaKeyBytes.length);
      ripemd.doFinal(publicRipemdKeyBytes, 0);

      // Add network bytes
      byte[] networkPublicKeyBytes = {(byte) 0x6F}; // TESTNET
      // byte[] networkPublicKeyBytes = { (byte)0x00 }; // REALNET
      byte[] networkPublicKeyBytes2 =
          new byte[networkPublicKeyBytes.length + publicRipemdKeyBytes.length];
      System.arraycopy(networkPublicKeyBytes, 0, networkPublicKeyBytes2, 0,
          networkPublicKeyBytes.length);
      System.arraycopy(publicRipemdKeyBytes, 0, networkPublicKeyBytes2,
          networkPublicKeyBytes.length, publicRipemdKeyBytes.length);
      networkPublicKeyBytes = networkPublicKeyBytes2.clone();

      md = MessageDigest.getInstance("SHA-256");
      md.reset();
      md.update(networkPublicKeyBytes);
      byte[] publicKeyChecksum = Arrays.copyOfRange(md.digest(md.digest()), 0, 4);

      byte[] decodedPublicKey = new byte[networkPublicKeyBytes.length + publicKeyChecksum.length];
      System.arraycopy(networkPublicKeyBytes, 0, decodedPublicKey, 0, networkPublicKeyBytes.length);
      System.arraycopy(publicKeyChecksum, 0, decodedPublicKey, networkPublicKeyBytes.length,
          publicKeyChecksum.length);

      String publicKey = Base58.encode(decodedPublicKey);
      return publicKey;

    } catch (Exception e) {
      System.out.println("Panic!!" + e.toString());
      e.printStackTrace(System.out);
      return null;
    }
  }

  public static String decodeAddress(String address) {
    try {
      byte[] decodedNetworkAddress = Base58.decode(address);
      byte[] networkBytes = ByteUtilities.readBytes(decodedNetworkAddress, 0, 1);
      byte[] addressBytes = new byte[] {};

      if ((networkBytes[0] & 0xFF) == 0x00 || (networkBytes[0] & 0xFF) == 0x6F
          || (networkBytes[0] & 0xFF) == 0x05 || (networkBytes[0] & 0xFF) == 0xC4) {
        addressBytes = ByteUtilities.readBytes(decodedNetworkAddress, 1, 20);
      } else if ((networkBytes[0] & 0xFF) == 0x80 || (networkBytes[0] & 0xFF) == 0xEF) {
        addressBytes = ByteUtilities.readBytes(decodedNetworkAddress, 1, 32);
      }

      // byte[] checksumBytes = ByteUtilities.readBytes(decodedNetworkAddress, 21/33, 4);
      // TODO - Verify checksum

      return ByteUtilities.toHexString(addressBytes);
    } catch (Exception e) {
      return "";
    }
  }

  public static boolean isMultiSigAddress(String address) {
    try {
      // If the address isn't valid.
      if (decodeAddress(address).isEmpty()) {
        return false;
      }

      byte[] decodedNetworkAddress = Base58.decode(address);
      byte[] networkBytes = ByteUtilities.readBytes(decodedNetworkAddress, 0, 1);

      if ((networkBytes[0] & 0xFF) == 0x05 || (networkBytes[0] & 0xFF) == 0xC4) {
        return true;
      }

      return false;

    } catch (Exception e) {
      return false;
    }
  }

  public static String getPublicKey(String privateKey) {
    return ByteUtilities.toHexString(getPublicKeyBytes(privateKey));
  }

  public static byte[] getPublicKeyBytes(String privateKey) {
    try {
      byte[] decodedPrivateKey = Base58.decode(privateKey);
      byte[] networkPrivateKeyBytes = new byte[decodedPrivateKey.length - 4]; // trailing
                                                                              // 4-byte
                                                                              // checksum
      byte[] privateKeyChecksum = new byte[4];

      System.arraycopy(decodedPrivateKey, 0, networkPrivateKeyBytes, 0,
          decodedPrivateKey.length - 4);
      System.arraycopy(decodedPrivateKey, decodedPrivateKey.length - 4, privateKeyChecksum, 0, 4);

      // Is it valid?
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(networkPrivateKeyBytes);
      byte[] checksumCheck = Arrays.copyOfRange(md.digest(md.digest()), 0, 4);
      for (int i = 0; i < 4; i++) {
        if (privateKeyChecksum[i] != checksumCheck[i]) {
          System.out.println("Bad Checksum");
          return null;
        }
      }

      // Strip leading network byte and get the public key
      byte[] privateKeyBytes =
          Arrays.copyOfRange(networkPrivateKeyBytes, 1, networkPrivateKeyBytes.length);
      byte[] publicKeyBytes = Secp256k1.getPublicKey(privateKeyBytes);

      return publicKeyBytes;

    } catch (Exception e) {
      System.out.println("Panic!!" + e.toString());
      e.printStackTrace(System.out);
      return null;
    }
  }
}
