package io.emax.cosigner.ethereum.token;

import io.emax.cosigner.ethereum.core.EthereumResource;
import io.emax.cosigner.ethereum.core.stubrpc.EthereumTestRpc;

import org.junit.Test;

public class TokensTest {
  @Test
  public void generateTokens() {
    EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
    TokenWallet wallet = new TokenWallet(new TokenConfiguration("EUR"));

    String generatedTx = wallet.generateTokens("0x1121212", 10000);
    System.out.println("TX to generate 100.00 worth of tokens: " + generatedTx);
  }

  @Test
  public void destroyTokens() {
    EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
    TokenWallet wallet = new TokenWallet(new TokenConfiguration("EUR"));

    String generatedTx = wallet.destroyTokens("0x1121212", 10000);
    System.out.println("TX to destroy 100.00 worth of tokens: " + generatedTx);
  }
}
