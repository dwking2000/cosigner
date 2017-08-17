package io.emax.cosigner.ethereum.tokenstorage;

import io.emax.cosigner.ethereum.core.EthereumResource;
import io.emax.cosigner.ethereum.core.stubrpc.EthereumTestRpc;

import org.junit.Test;

public class TokensTest {
  @Test
  public void generateTokens() {
    EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
    Wallet wallet = new Wallet(new Configuration("EUR"));

    String generatedTx = Utilities.generateTokens("0x1121212", 10000, wallet.config);
    System.out.println("TX to generate 100.00 worth of tokens: " + generatedTx);
  }

  @Test
  public void destroyTokens() {
    EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
    Wallet wallet = new Wallet(new Configuration("EUR"));

    String generatedTx = Utilities.destroyTokens("0x1121212", 10000, wallet.config);
    System.out.println("TX to destroy 100.00 worth of tokens: " + generatedTx);
  }
}
