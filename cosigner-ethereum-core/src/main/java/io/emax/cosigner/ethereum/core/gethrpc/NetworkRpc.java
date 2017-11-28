package io.emax.cosigner.ethereum.core.gethrpc;

public interface NetworkRpc {
  String eth_blockNumber() throws Exception;

  String eth_call(CallData object, String defaultBlock) throws Exception;

  Block eth_getBlockByNumber(String number, boolean returnTxData) throws Exception;

  String net_version() throws Exception;
}
