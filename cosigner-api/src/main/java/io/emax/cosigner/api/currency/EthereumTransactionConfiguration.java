package io.emax.cosigner.api.currency;

public interface EthereumTransactionConfiguration {
  long getGasPrice();

  long getSimpleTxGas();

  long getContractGas();

  long getWeiMultiplier();
}
