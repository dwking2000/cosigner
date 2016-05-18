package io.emax.cosigner.ethereum.gethrpc;

public interface NetworkRpc {
  String eth_blockNumber();

  String eth_call(CallData object, String defaultBlock);

  Block eth_getBlockByNumber(String number, boolean returnTxData);

  String net_version();
}
