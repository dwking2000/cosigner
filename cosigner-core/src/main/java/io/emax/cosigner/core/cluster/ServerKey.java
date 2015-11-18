package io.emax.cosigner.core.cluster;

import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.crypto.Secp256k1;

import org.bouncycastle.crypto.digests.SHA3Digest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;

public class ServerKey {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerKey.class);
  private static final byte[] myKey = Secp256k1.generatePrivateKey();

  public static String getServerId() {
    return ByteUtilities.toHexString(Secp256k1.getPublicKey(myKey));
  }

  /**
   * Signs the server's public ID with the cluster key.
   */
  public static String[] getClusterSignature(String clusterKey) {
    LinkedList<String> rsvValues = new LinkedList<>();
    while (rsvValues.isEmpty()) {
      // Hash the public key
      SHA3Digest sha3 = new SHA3Digest(256);
      sha3.reset();
      sha3.update(Secp256k1.getPublicKey(myKey), 0, Secp256k1.getPublicKey(myKey).length);
      byte[] hashedBytes = new byte[256 / 8];
      sha3.doFinal(hashedBytes, 0);

      // Sign it.
      byte[][] signature =
          Secp256k1.signTransaction(hashedBytes, ByteUtilities.toByteArray(clusterKey));
      Arrays.asList(signature).forEach(sigValue -> {
        rsvValues.add(ByteUtilities.toHexString(sigValue));
      });

      // Recovery ID is bad, try again.
      if (rsvValues.get(2).equalsIgnoreCase("ff")) {
        LOGGER.debug("Problem getting signature, V is invalid.");
        LOGGER.debug(rsvValues.toString());
        rsvValues.clear();
      }
    }

    return rsvValues.toArray(new String[0]);
  }

  /**
   * Returns the server's key.
   */
  public static byte[] getMykey() {
    byte[] returnArray = new byte[myKey.length];
    System.arraycopy(myKey, 0, returnArray, 0, myKey.length);
    return returnArray;
  }
}
