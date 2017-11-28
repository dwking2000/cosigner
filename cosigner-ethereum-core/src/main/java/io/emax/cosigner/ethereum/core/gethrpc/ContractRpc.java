package io.emax.cosigner.ethereum.core.gethrpc;

public interface ContractRpc {
  String eth_getCode(String address, String defaultBlock) throws Exception;

  String eth_getStorageAt(String address, String position, String defaultBlock) throws Exception;
}
