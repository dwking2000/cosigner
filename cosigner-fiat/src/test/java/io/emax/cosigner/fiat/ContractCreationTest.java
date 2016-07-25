package io.emax.cosigner.fiat;

import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.ethereum.EthereumResource;
import io.emax.cosigner.ethereum.stubrpc.EthereumTestRpc;

import org.junit.Test;

public class ContractCreationTest {
  @Test
  public void createContract() {
    // Find or create a contract. Bytecode test more than anything.
    EthereumResource.getResource().setEthereumRpc(new EthereumTestRpc());
    Wallet wallet = new FiatWallet(new FiatConfiguration("EUR"));
  }
}
