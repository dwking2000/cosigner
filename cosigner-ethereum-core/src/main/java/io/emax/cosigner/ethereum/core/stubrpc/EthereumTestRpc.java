package io.emax.cosigner.ethereum.core.stubrpc;

import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.ethereum.core.common.EthereumTools;
import io.emax.cosigner.ethereum.core.gethrpc.Block;
import io.emax.cosigner.ethereum.core.gethrpc.CallData;
import io.emax.cosigner.ethereum.core.gethrpc.EthereumRpc;
import io.emax.cosigner.ethereum.core.gethrpc.multisig.MultiSigContract;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class EthereumTestRpc implements EthereumRpc {
  private int txCounter = 1;

  @Override
  public String eth_getBalance(String address, String defaultBlock) {
    return "0x15AF1D78B58C40000";
  }

  @Override
  public String eth_getTransactionCount(String address, String defaultBlock) {
    return ByteUtilities.toHexString(
        ByteUtilities.stripLeadingNullBytes(BigInteger.valueOf(txCounter).toByteArray()));
  }

  @Override
  public Map<String, Object> eth_getTransactionByHash(String txid) {
    return null;
  }

  @Override
  public Map<String, Object> eth_getTransactionReceipt(String txid) {
    Map<String, Object> receiptData = new HashMap<>();

    receiptData.put("contractAddress", "0x123456789");
    receiptData.put("blockNumber", "0x04");
    return receiptData;
  }

  @Override
  public String eth_newFilter(Map<String, Object> filterParams) {
    return "FILTER";
  }

  @Override
  public Map<String, Object>[] eth_getFilterChanges(String filterId) {
    return null;
  }

  @Override
  public Map<String, Object>[] eth_getFilterLogs(String filterId) {
    return new Map[]{};
  }

  @Override
  public Boolean eth_uninstallFilter(String filterId) {
    return true;
  }

  @Override
  public Map<String, Object>[] eth_getLogs(String filterId) {
    return null;
  }

  @Override
  public String eth_getCode(String address, String defaultBlock) {
    return "0x" + new MultiSigContract().getContractPayload();
  }

  @Override
  public String eth_getStorageAt(String address, String position, String defaultBlock) {
    return "0x5506c24faeb93b11b079814eadff7283368f287b";
  }

  @Override
  public String eth_sendRawTransaction(String transaction) {
    txCounter++;
    return EthereumTools.hashKeccak(transaction);
  }

  @Override
  public String eth_sign(String address, String data) {
    return "0x12345678901234567890123456789012345678901234567890123456789"
        + "01234112345678901234567890123456789012345678901234567890123456789012341";
  }

  @Override
  public String eth_blockNumber() {
    return "0x0255";
  }

  @Override
  public String eth_call(CallData object, String defaultBlock) {
    return null;
  }

  @Override
  public Block eth_getBlockByNumber(String number, boolean returnTxData) {
    return null;
  }

  @Override
  public String net_version() {
    return "1";
  }
}
