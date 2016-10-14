package io.emax.cosigner.ethereum.core.gethrpc;

import java.util.Map;

public interface WalletRpc {
  String eth_getBalance(String address, String defaultBlock);

  String eth_getTransactionCount(String address, String defaultBlock);

  Map<String, Object> eth_getTransactionByHash(String txid);

  Map<String, Object> eth_getTransactionReceipt(String txid);

  String eth_newFilter(Map<String, Object> filterParams);

  Map<String, Object>[] eth_getFilterChanges(String filterId);

  Map<String, Object>[] eth_getFilterLogs(String filterId);

  Map<String, Object>[] eth_getLogs(String filterId);
}
