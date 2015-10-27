package io.emax.cosigner.core.cluster.crypto;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Aes {
  private static final Logger logger = LoggerFactory.getLogger(Aes.class);

  /**
   * Encrypt data using the provided key and IV data.
   */
  public static String encrypt(byte[] key, byte[] iv, String encString) {
    try {
      return Aes.transform(key, iv, encString, true);
    } catch (Exception e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      logger.error(errors.toString());
      return "";
    }
  }

  /**
   * Decrypt data using the provided key and IV data.
   */
  public static String decrypt(byte[] key, byte[] iv, String encString) {
    try {
      return Aes.transform(key, iv, encString, false);
    } catch (Exception e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      logger.error(errors.toString());
      return "";
    }
  }

  private static String transform(byte[] key, byte[] iv, String encString, boolean encrypt)
      throws Exception {

    // setup cipher parameters with key and IV
    KeyParameter keyParam = new KeyParameter(key);
    CipherParameters params = new ParametersWithIV(keyParam, iv);

    // setup AES cipher in CBC mode with PKCS7 padding
    BlockCipherPadding padding = new PKCS7Padding();
    BufferedBlockCipher cipher =
        new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);
    cipher.reset();
    cipher.init(encrypt, params);

    // create a temporary buffer to decode into (it'll include padding)
    byte[] encData = ByteUtilities.toByteArray(encString);
    byte[] buf = new byte[cipher.getOutputSize(encData.length)];
    int len = cipher.processBytes(encData, 0, encData.length, buf, 0);
    len += cipher.doFinal(buf, len);

    // remove padding
    byte[] out = new byte[len];
    System.arraycopy(buf, 0, out, 0, len);

    // return string representation of decoded bytes
    return ByteUtilities.toHexString(out);
  }
}
