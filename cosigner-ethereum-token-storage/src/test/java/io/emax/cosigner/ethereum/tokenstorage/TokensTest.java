package io.emax.cosigner.ethereum.tokenstorage;

import io.emax.cosigner.ethereum.core.EthereumResource;
import io.emax.cosigner.ethereum.core.stubrpc.EthereumTestRpc;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.logging.Logger;

public class TokensTest {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TokensTest.class);

  @Test
  public void generateTokens() {
    try {
      EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
      Wallet wallet = new Wallet(new Configuration("EUR"));

      String generatedTx = Utilities.generateTokens("0x1121212", 10000, wallet.config);
      System.out.println("TX to generate 100.00 worth of tokens: " + generatedTx);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Problem generating tokens");
    }
  }

  @Test
  public void destroyTokens() {
    try {
      EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
      Wallet wallet = new Wallet(new Configuration("EUR"));

      String generatedTx = Utilities.destroyTokens("0x1121212", 10000, wallet.config);
      System.out.println("TX to destroy 100.00 worth of tokens: " + generatedTx);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Problem destroying tokens");
    }
  }
}
