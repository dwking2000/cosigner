package io.emax.cosigner.ethereum.core.gethrpc;

public interface RawTransactionRpc {
  String eth_sendRawTransaction(String transaction) throws Exception;

  String eth_sign(String address, String data) throws Exception;
}
