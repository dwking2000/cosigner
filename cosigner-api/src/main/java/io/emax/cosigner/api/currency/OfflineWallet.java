package io.emax.cosigner.api.currency;

public interface OfflineWallet {
  /**
   * Generate a private key compatible with the wallet.
   */
  String generatePrivateKey();

  /**
   * Generate public key from a private one.
   */
  String generatePublicKey(String privateKey);

  /**
   * Sign the provided transaction with a private key.
   */
  Iterable<Iterable<String>> signWithPrivateKey(Iterable<Iterable<String>> data, String privateKey);
}
