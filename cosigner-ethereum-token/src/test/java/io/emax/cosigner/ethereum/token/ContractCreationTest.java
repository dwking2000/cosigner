package io.emax.cosigner.ethereum.token;

import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.ethereum.core.EthereumResource;
import io.emax.cosigner.ethereum.core.stubrpc.EthereumTestRpc;

import org.junit.Test;

public class ContractCreationTest {
  @Test
  public void createContract() {
    // Find or create a contract. Bytecode test more than anything.
    EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
    Wallet wallet = new TokenWallet(new TokenConfiguration("EUR"));
  }
}
