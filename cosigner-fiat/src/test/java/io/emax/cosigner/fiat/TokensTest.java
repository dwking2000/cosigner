package io.emax.cosigner.fiat;

import io.emax.cosigner.ethereum.EthereumResource;
import io.emax.cosigner.ethereum.stubrpc.EthereumTestRpc;

import org.junit.Test;

public class TokensTest {
  @Test
  public void generateTokens() {
    EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
    FiatWallet wallet = new FiatWallet(new FiatConfiguration("EUR"));

    String generatedTx = wallet.generateTokens(10000);
    System.out.println("TX to generate 100.00 worth of tokens: " + generatedTx);
  }

  @Test
  public void destroyTokens() {
    EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
    FiatWallet wallet = new FiatWallet(new FiatConfiguration("EUR"));

    String generatedTx = wallet.destroyTokens(10000);
    System.out.println("TX to destroy 100.00 worth of tokens: " + generatedTx);
  }
}
