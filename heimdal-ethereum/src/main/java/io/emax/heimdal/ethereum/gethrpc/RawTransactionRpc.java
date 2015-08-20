package io.emax.heimdal.ethereum.gethrpc;

public interface RawTransactionRpc {
  String eth_sendRawTransaction(String transaction);

  String eth_sign(String address, String data);
}
