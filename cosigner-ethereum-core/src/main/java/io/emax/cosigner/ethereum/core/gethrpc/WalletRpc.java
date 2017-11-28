package io.emax.cosigner.ethereum.core.gethrpc;

import java.util.Map;

public interface WalletRpc {
  String eth_getBalance(String address, String defaultBlock) throws Exception;

  String eth_getTransactionCount(String address, String defaultBlock) throws Exception;

  Map<String, Object> eth_getTransactionByHash(String txid) throws Exception;

  Map<String, Object> eth_getTransactionReceipt(String txid) throws Exception;

  String eth_newFilter(Map<String, Object> filterParams) throws Exception;

  Map<String, Object>[] eth_getFilterChanges(String filterId) throws Exception;

  Map<String, Object>[] eth_getFilterLogs(String filterId) throws Exception;

  Boolean eth_uninstallFilter(String filterId) throws Exception;

  Map<String, Object>[] eth_getLogs(String filterId) throws Exception;
}
