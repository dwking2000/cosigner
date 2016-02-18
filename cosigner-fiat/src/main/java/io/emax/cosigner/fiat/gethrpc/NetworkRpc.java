package io.emax.cosigner.fiat.gethrpc;

public interface NetworkRpc {
  String eth_blockNumber();

  String eth_call(CallData object, String defaultBlock);

  Block eth_getBlockByNumber(String number, boolean returnTxData);
}
