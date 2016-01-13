package io.emax.cosigner.common.crypto;

import io.emax.cosigner.common.ByteUtilities;

import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

public class CryptoTest {
  @Test
  public void testSharedSecret() throws UnsupportedEncodingException {
    System.out.println("Testing shared secret.");
    byte[] myKey = Secp256k1.generatePrivateKey();
    byte[] myPublicKey = Secp256k1.getPublicKey(myKey);

    byte[] otherKey = Secp256k1.generatePrivateKey();
    byte[] otherPublicKey = Secp256k1.getPublicKey(otherKey);

    byte[] mySharedSecret = Secp256k1.generateSharedSecret(myKey, otherPublicKey);
    byte[] otherSharedSecret = Secp256k1.generateSharedSecret(otherKey, myPublicKey);

    // Verify that shared secrets match both directions
    String mySecret = ByteUtilities.toHexString(mySharedSecret);
    String otherSecret = ByteUtilities.toHexString(otherSharedSecret);
    System.out.println("My Secret: " + mySecret);
    System.out.println("Other secret: " + otherSecret);
    Assert.assertEquals(mySecret, otherSecret);

    byte[] iv = Aes.generateIv();

    String encryptThis = "THISISATEST";
    System.out.println("Original data: " + encryptThis);
    String encryptedData =
        Aes.encrypt(mySharedSecret, iv, ByteUtilities.toHexString(encryptThis.getBytes("UTF-8")));
    System.out.println("Encrypted with my key: " + encryptedData);
    String decryptedData = Aes.decrypt(otherSharedSecret, iv, encryptedData);
    decryptedData = new String(ByteUtilities.toByteArray(decryptedData), "UTF-8");
    System.out.println("Decrypted with the other key: " + decryptedData);

    Assert.assertEquals(encryptThis, decryptedData);
  }
}
