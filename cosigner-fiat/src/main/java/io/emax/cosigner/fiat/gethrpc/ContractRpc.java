package io.emax.cosigner.fiat.gethrpc;

public interface ContractRpc {
  String eth_getCode(String address, String defaultBlock);

  String eth_getStorageAt(String address, String position, String defaultBlock);
}
