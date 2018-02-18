package io.emax.cosigner.ethereum.tokenstorage;

import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.ethereum.core.gethrpc.Block;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static io.emax.cosigner.ethereum.tokenstorage.Base.ethereumReadRpc;
import static io.emax.cosigner.ethereum.tokenstorage.Base.ethereumWriteRpc;

public class Filters {
  private static final Logger LOGGER = LoggerFactory.getLogger(Filters.class);

  private static class TxDateComparator implements Comparator<Wallet.TransactionDetails> {
    @Override
    public int compare(Wallet.TransactionDetails o1, Wallet.TransactionDetails o2) {
      return o1.getTxDate().compareTo(o2.getTxDate());
    }
  }

  private static HashMap<String, LinkedList<Map<String, Object>>>
      cachedReconciliationFilterResults = new HashMap<>();
  private static long maxReconcilationBlocks = 5000;
  private static HashMap<String, Long> lastReconciliationBlock = new HashMap<>();

  synchronized static Wallet.TransactionDetails[] getReconciliations(String address,
      Configuration config) throws Exception {

    if (!lastReconciliationBlock.containsKey(config.getStorageContractAddress())) {
      lastReconciliationBlock.put(config.getStorageContractAddress(), 0L);
    }

    // Get latest block
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumReadRpc.eth_blockNumber()));

    LinkedList<Wallet.TransactionDetails> txDetails = new LinkedList<>();
    Map<String, Object> filterParams = new HashMap<>();
    String startBlock =
        "0x" + Long.toHexString(lastReconciliationBlock.get(config.getStorageContractAddress()));
    String endBlock = "0x" + Long.toHexString(Math.min(latestBlockNumber.longValue(),
        lastReconciliationBlock.get(config.getStorageContractAddress()) + maxReconcilationBlocks));
    filterParams.put("fromBlock", startBlock);
    filterParams.put("toBlock", endBlock);
    filterParams.put("address", "0x" + config.getStorageContractAddress());
    LinkedList<String> functionTopics = new LinkedList<>();
    functionTopics.add("0x73bb00f3ad09ef6bc524e5cf56563dff2bc6663caa0b4054aa5946811083ed2e");
    Map<String, Object>[] filterResults = new Map[0];
    for (String functionTopic : functionTopics) {
      Object[] topicArray = new Object[1];
      String[] senderTopic = {functionTopic};
      topicArray[0] = senderTopic;
      filterParams.put("topics", topicArray);
      LOGGER.debug(
          "Requesting reconciliation filter for: " + Json.stringifyObject(Map.class, filterParams));
      String txFilter = "";
      LOGGER.debug("Setup filter: " + txFilter);
      boolean filterSucceeded = true;
      try {
        LOGGER.debug("Getting filter results...");
        txFilter = ethereumReadRpc.eth_newFilter(filterParams);
        filterResults = ethereumReadRpc.eth_getFilterLogs(txFilter);

        if (filterResults == null) {
          filterResults = new Map[0];
        }
      } catch (Exception e) {
        LOGGER.debug("Something went wrong", e);
        filterResults = new Map[0];
        filterSucceeded = false;
      } finally {
        if (filterSucceeded) {
          if (cachedReconciliationFilterResults.containsKey(config.getStorageContractAddress())) {
            LinkedList<Map<String, Object>> addressResults =
                cachedReconciliationFilterResults.get(config.getStorageContractAddress());
            addressResults.addAll(Arrays.asList(filterResults));
            cachedReconciliationFilterResults
                .put(config.getStorageContractAddress(), addressResults);
          } else {
            cachedReconciliationFilterResults.put(config.getStorageContractAddress(),
                new LinkedList<>(Arrays.asList(filterResults)));
          }
        }
        ethereumWriteRpc.eth_uninstallFilter(txFilter);
      }
    }

    long newLastBlock =
        lastReconciliationBlock.get(config.getStorageContractAddress()) + maxReconcilationBlocks;
    lastReconciliationBlock.put(config.getStorageContractAddress(),
        Math.min(newLastBlock, latestBlockNumber.longValue()));

    filterResults = cachedReconciliationFilterResults.get(config.getStorageContractAddress())
        .toArray(filterResults);
    for (Map<String, Object> result : filterResults) {
      LOGGER.debug(result.toString());
      Wallet.TransactionDetails txDetail = new Wallet.TransactionDetails();
      txDetail.setTxHash((String) result.get("transactionHash"));
      try {
        Block block =
            ethereumReadRpc.eth_getBlockByNumber((String) result.get("blockNumber"), true);
        BigInteger dateConverter =
            new BigInteger(1, ByteUtilities.toByteArray(block.getTimestamp()));
        dateConverter = dateConverter.multiply(BigInteger.valueOf(1000));
        txDetail.setTxDate(new Date(dateConverter.longValue()));

        BigInteger txBlockNumber =
            new BigInteger(1, ByteUtilities.toByteArray((String) result.get("blockNumber")));
        txDetail.setConfirmed(
            config.getMinConfirmations() <= latestBlockNumber.subtract(txBlockNumber).intValue());
        txDetail.setConfirmations(latestBlockNumber.subtract(txBlockNumber).intValue());
        txDetail.setMinConfirmations(config.getMinConfirmations());

        ArrayList<String> topics = (ArrayList<String>) result.get("topics");

        boolean skipResult = true;
        for (String functionTopic : functionTopics) {
          if (topics.get(0).equalsIgnoreCase(functionTopic)) {
            skipResult = false;
          }
        }

        if (skipResult) {
          continue;
        }

        String amount = ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(ByteUtilities
            .readBytes(ByteUtilities.toByteArray((String) result.get("data")), 0, 32)));
        txDetail.setAmount(new BigDecimal(new BigInteger(ByteUtilities.toByteArray(amount)))
            .setScale(20, BigDecimal.ROUND_UNNECESSARY)
            .divide(BigDecimal.valueOf(10).pow((int) config.getDecimalPlaces()),
                BigDecimal.ROUND_UNNECESSARY));

        if (BigInteger.ZERO.compareTo(new BigInteger(ByteUtilities.toByteArray(amount))) > 0) {
          String from = ByteUtilities.toHexString(
              ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(1))));
          txDetail.setFromAddress(new String[]{from});
        } else {
          String to = ByteUtilities.toHexString(
              ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(1))));
          txDetail.setToAddress(new String[]{to});
        }

        if (address == null || ByteUtilities.toHexString(
            ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(1))))
            .equalsIgnoreCase(address)) {
          txDetails.add(txDetail);
        }

      } catch (Exception e) {
        // Pending TX
        LOGGER.debug("Pending Tx Found or wrong event returned by geth.");
        LOGGER.trace(null, e);
      }
    }


    LOGGER.debug(Json.stringifyObject(LinkedList.class, txDetails));
    Collections.sort(txDetails, new Filters.TxDateComparator());
    return txDetails.toArray(new Wallet.TransactionDetails[txDetails.size()]);
  }

  private static HashMap<String, LinkedList<Map<String, Object>>> cachedTransferFilterResults =
      new HashMap<>();
  private static long maxTransferBlocks = 5000;
  private static HashMap<String, Long> lastTransferBlock = new HashMap<>();

  synchronized static Wallet.TransactionDetails[] getTransfers(String address, Configuration config)
      throws Exception {

    if (!lastTransferBlock.containsKey(config.getStorageContractAddress())) {
      lastTransferBlock.put(config.getStorageContractAddress(), 0L);
    }
    // Get latest block
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumReadRpc.eth_blockNumber()));

    LinkedList<Wallet.TransactionDetails> txDetails = new LinkedList<>();
    Map<String, Object> filterParams = new HashMap<>();
    String startBlock =
        "0x" + Long.toHexString(lastTransferBlock.get(config.getStorageContractAddress()));
    String endBlock = "0x" + Long.toHexString(Math.min(latestBlockNumber.longValue(),
        lastTransferBlock.get(config.getStorageContractAddress()) + maxTransferBlocks));
    filterParams.put("fromBlock", startBlock);
    filterParams.put("toBlock", endBlock);
    filterParams.put("address", "0x" + config.getStorageContractAddress());
    LinkedList<String> functionTopics = new LinkedList<>();
    functionTopics.add("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef");
    functionTopics.add("0x5548c837ab068cf56a2c2479df0882a4922fd203edb7517321831d95078c5f62");
    Map<String, Object>[] filterResults = new Map[0];

    for (String functionTopic : functionTopics) {
      Object[] topicArray = new Object[1];
      String[] senderTopic = {functionTopic};
      topicArray[0] = senderTopic;
      filterParams.put("topics", topicArray);
      LOGGER.debug("Requesting filter for: " + Json.stringifyObject(Map.class, filterParams));
      String txFilter = "";

      LOGGER.debug("Setup filter: " + txFilter);
      boolean filterSucceeded = true;
      try {
        LOGGER.debug("Getting filter results...");
        txFilter = ethereumReadRpc.eth_newFilter(filterParams);
        filterResults = ethereumReadRpc.eth_getFilterLogs(txFilter);

        if (filterResults == null) {
          filterResults = new Map[0];
        }
      } catch (Exception e) {
        LOGGER.debug("Something went wrong", e);
        filterResults = new Map[0];
        filterSucceeded = false;
      } finally {
        if (filterSucceeded) {
          if (cachedTransferFilterResults.containsKey(config.getStorageContractAddress())) {
            LinkedList<Map<String, Object>> addressResults =
                cachedTransferFilterResults.get(config.getStorageContractAddress());
            addressResults.addAll(Arrays.asList(filterResults));
            cachedTransferFilterResults.put(config.getStorageContractAddress(), addressResults);
          } else {
            cachedTransferFilterResults.put(config.getStorageContractAddress(),
                new LinkedList<>(Arrays.asList(filterResults)));
          }
        }
        ethereumWriteRpc.eth_uninstallFilter(txFilter);
      }
    }

    long newLastBlock =
        lastTransferBlock.get(config.getStorageContractAddress()) + maxTransferBlocks;
    lastTransferBlock.put(config.getStorageContractAddress(),
        Math.min(newLastBlock, latestBlockNumber.longValue()));

    filterResults =
        cachedTransferFilterResults.get(config.getStorageContractAddress()).toArray(filterResults);
    for (Map<String, Object> result : filterResults) {
      LOGGER.debug(result.toString());
      Wallet.TransactionDetails txDetail = new Wallet.TransactionDetails();
      txDetail.setTxHash((String) result.get("transactionHash"));
      try {
        Block block =
            ethereumReadRpc.eth_getBlockByNumber((String) result.get("blockNumber"), true);
        BigInteger dateConverter =
            new BigInteger(1, ByteUtilities.toByteArray(block.getTimestamp()));
        dateConverter = dateConverter.multiply(BigInteger.valueOf(1000));
        txDetail.setTxDate(new Date(dateConverter.longValue()));

        BigInteger txBlockNumber =
            new BigInteger(1, ByteUtilities.toByteArray((String) result.get("blockNumber")));
        txDetail.setConfirmed(
            config.getMinConfirmations() <= latestBlockNumber.subtract(txBlockNumber).intValue());
        txDetail.setConfirmations(latestBlockNumber.subtract(txBlockNumber).intValue());
        txDetail.setMinConfirmations(config.getMinConfirmations());

        ArrayList<String> topics = (ArrayList<String>) result.get("topics");

        boolean skipResult = true;
        for (String functionTopic : functionTopics) {
          if (topics.get(0).equalsIgnoreCase(functionTopic)) {
            skipResult = false;
          }
        }

        if (skipResult) {
          continue;
        }

        String from = ByteUtilities.toHexString(
            ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(1))));
        txDetail.setFromAddress(new String[]{from});

        String to = ByteUtilities.toHexString(
            ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(2))));
        txDetail.setToAddress(new String[]{to});

        String amount = ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(ByteUtilities
            .readBytes(ByteUtilities.toByteArray((String) result.get("data")), 0, 32)));
        txDetail.setAmount(new BigDecimal(new BigInteger(1, ByteUtilities.toByteArray(amount)))
            .setScale(20, BigDecimal.ROUND_UNNECESSARY)
            .divide(BigDecimal.valueOf(10).pow((int) config.getDecimalPlaces()),
                BigDecimal.ROUND_UNNECESSARY));

        if (address == null || ByteUtilities.toHexString(
            ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(1))))
            .equalsIgnoreCase(address) || ByteUtilities.toHexString(
            ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(2))))
            .equalsIgnoreCase(address)) {
          txDetails.add(txDetail);
        }
      } catch (Exception e) {
        // Pending TX
        LOGGER.debug("Pending Tx Found or wrong event returned by geth.");
        LOGGER.trace(null, e);
      }
    }

    LOGGER.debug(Json.stringifyObject(LinkedList.class, txDetails));
    Collections.sort(txDetails, new Filters.TxDateComparator());
    return txDetails.toArray(new Wallet.TransactionDetails[txDetails.size()]);
  }
}
