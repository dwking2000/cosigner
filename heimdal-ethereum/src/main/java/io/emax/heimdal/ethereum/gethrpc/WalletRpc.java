package io.emax.heimdal.ethereum.gethrpc;

public interface WalletRpc {
  public String eth_getBalance(String address, String defaultBlock);
  public String eth_getTransactionCount(String address, String defaultBlock);
}
