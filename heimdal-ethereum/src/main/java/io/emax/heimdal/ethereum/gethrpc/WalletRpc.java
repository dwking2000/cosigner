package io.emax.heimdal.ethereum.gethrpc;

public interface WalletRpc {
  String eth_getBalance(String address, String defaultBlock);

  String eth_getTransactionCount(String address, String defaultBlock);

  String eth_blockNumber();

  String eth_sendRawTransaction(String transaction);

  String eth_sign(String address, String data);

  String eth_getCode(String address, String defaultBlock);

  String eth_getStorageAt(String address, String position, String defaultBlock);
}
