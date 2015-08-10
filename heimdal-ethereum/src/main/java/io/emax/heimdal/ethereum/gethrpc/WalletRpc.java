package io.emax.heimdal.ethereum.gethrpc;

public interface WalletRpc {
  public String eth_getBalance(String address, String defaultBlock);
}
