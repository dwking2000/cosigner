package io.emax.cosigner.ethereum.core;

import io.emax.cosigner.ethereum.core.stubrpc.EthereumTestRpc;

import org.junit.Assert;
import org.junit.Test;

public class CryptoTest {
  @Test
  public void privateKeyToPublicKey() {
    EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
    EthereumWallet wallet = new EthereumWallet(new EthereumConfiguration());

    String privateKey = "a833887c23e530c44d3677caabd701bacc7514fb9a0ab59a22920b5559a03cad";
    System.out.println("PrivateKey: " + privateKey);

    String publicKey = wallet.generatePublicKey(privateKey);
    System.out.println("Public key: " + publicKey);

    String pubKeyAddress = wallet.createAddressFromKey(publicKey, false);
    String privKeyAddress = wallet.createAddressFromKey(privateKey, true);

    System.out.println("Calculcated addresses: " + pubKeyAddress + " + " + privKeyAddress);
    Assert.assertEquals(pubKeyAddress, privKeyAddress);
    Assert.assertEquals("23b72907206f620bc7599ec5fee8ae16aa405a57", pubKeyAddress);
  }
}
