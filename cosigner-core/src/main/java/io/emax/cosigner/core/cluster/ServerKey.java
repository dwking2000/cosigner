package io.emax.cosigner.core.cluster;

import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.crypto.Secp256k1;

import org.bouncycastle.crypto.digests.SHA3Digest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;

public class ServerKey {
  private static final Logger logger = LoggerFactory.getLogger(ServerKey.class);
  private static final byte[] myKey = Secp256k1.generatePrivateKey();

  public static String getServerId() {
    return ByteUtilities.toHexString(Secp256k1.getPublicKey(myKey));
  }

  /**
   * Signs the server's public ID with the cluster key.
   */
  public static String[] getClusterSignature(String clusterKey) {
    byte[][] signature = new byte[0][0];
    LinkedList<String> rsvValues = new LinkedList<>();
    while (rsvValues.isEmpty()) {
      // Hash the public key
      SHA3Digest sha3 = new SHA3Digest(256);
      sha3.reset();
      sha3.update(Secp256k1.getPublicKey(myKey), 0, Secp256k1.getPublicKey(myKey).length);
      byte[] hashedBytes = new byte[256 / 8];
      sha3.doFinal(hashedBytes, 0);

      // Sign it.
      signature = Secp256k1.signTransaction(hashedBytes, ByteUtilities.toByteArray(clusterKey));
      Arrays.asList(signature).forEach(sigValue -> {
        rsvValues.add(ByteUtilities.toHexString(sigValue));
      });

      // Recovery ID is bad, try again.
      if (rsvValues.get(2).equalsIgnoreCase("ff")) {
        logger.debug("Problem getting signature, V is invalid.");
        logger.debug(rsvValues.toString());
        rsvValues.clear();
      }
    }

    return rsvValues.toArray(new String[0]);
  }

  public static byte[] getMykey() {
    return myKey;
  }
}
