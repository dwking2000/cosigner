package io.emax.cosigner.ethereum.tokenstorage;

import io.emax.cosigner.ethereum.core.EthereumResource;
import io.emax.cosigner.ethereum.core.stubrpc.EthereumTestRpc;

import org.junit.Test;

public class ContractCreationTest {
  @Test
  public void createContract() {
    // Find or create a contract. Bytecode test more than anything.
    EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
    io.emax.cosigner.api.currency.Wallet wallet = new Wallet(new Configuration("EUR"));
  }
}
