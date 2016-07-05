package io.emax.cosigner.ethereum.gethrpc;

import java.util.Map;

public interface WalletRpc {
  String eth_getBalance(String address, String defaultBlock);

  String eth_getTransactionCount(String address, String defaultBlock);

  Map<String, Object> eth_getTransactionByHash(String txid);
}
