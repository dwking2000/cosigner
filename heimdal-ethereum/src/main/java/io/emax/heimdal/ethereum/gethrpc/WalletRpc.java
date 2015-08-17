package io.emax.heimdal.ethereum.gethrpc;

public interface WalletRpc {
  public String eth_getBalance(String address, String defaultBlock);

  public String eth_getTransactionCount(String address, String defaultBlock);

  public String eth_blockNumber();

  public String eth_sendRawTransaction(String transaction);

  public String eth_sign(String address, String data);
  
  public String eth_getCode(String address, String defaultBlock);
  
  public String eth_getStorageAt(String address, String position, String defaultBlock);
}
