package io.emax.heimdal.ethereum.gethrpc;

public interface NetworkRpc {
  String eth_blockNumber();
  String eth_call(CallData object, String defaultBlock);
}
