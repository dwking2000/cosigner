package io.emax.cosigner.fiat.gethrpc;

public interface WalletRpc {
  String eth_getBalance(String address, String defaultBlock);

  String eth_getTransactionCount(String address, String defaultBlock);
}
