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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static io.emax.cosigner.ethereum.tokenstorage.Base.ethereumRpc;

public class Filters {
  private static final Logger LOGGER = LoggerFactory.getLogger(Filters.class);

  private static class TxDateComparator implements Comparator<Wallet.TransactionDetails> {
    @Override
    public int compare(Wallet.TransactionDetails o1, Wallet.TransactionDetails o2) {
      return o1.getTxDate().compareTo(o2.getTxDate());
    }
  }

  static Wallet.TransactionDetails[] getReconciliations(String address, Configuration config) {
    // Get latest block
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumRpc.eth_blockNumber()));

    LinkedList<Wallet.TransactionDetails> txDetails = new LinkedList<>();
    Map<String, Object> filterParams = new HashMap<>();
    filterParams.put("fromBlock", "0x0");
    filterParams.put("toBlock", "latest");
    filterParams.put("address", "0x" + config.getStorageContractAddress());
    LinkedList<String> functionTopics = new LinkedList<>();
    functionTopics.add("0x73bb00f3ad09ef6bc524e5cf56563dff2bc6663caa0b4054aa5946811083ed2e");
    for (String functionTopic : functionTopics) {
      Object[] topicArray = new Object[1];
      String[] senderTopic = {functionTopic};
      topicArray[0] = senderTopic;
      filterParams.put("topics", topicArray);
      LOGGER.debug(
          "Requesting reconciliation filter for: " + Json.stringifyObject(Map.class, filterParams));
      String txFilter = ethereumRpc.eth_newFilter(filterParams);
      LOGGER.debug("Setup filter: " + txFilter);
      Map<String, Object>[] filterResults;
      try {
        LOGGER.debug("Getting filter results...");
        filterResults = ethereumRpc.eth_getFilterLogs(txFilter);
      } catch (Exception e) {
        LOGGER.debug("Something went wrong", e);
        filterResults = new Map[0];
      }
      for (Map<String, Object> result : filterResults) {
        LOGGER.debug(result.toString());
        Wallet.TransactionDetails txDetail = new Wallet.TransactionDetails();
        txDetail.setTxHash((String) result.get("transactionHash"));
        try {
          Block block = ethereumRpc.eth_getBlockByNumber((String) result.get("blockNumber"), true);
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

          if (!topics.get(0).equalsIgnoreCase(functionTopic)) {
            continue;
          }

          String amount = ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(
              ByteUtilities
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
    }

    LOGGER.debug(Json.stringifyObject(LinkedList.class, txDetails));
    Collections.sort(txDetails, new Filters.TxDateComparator());
    return txDetails.toArray(new Wallet.TransactionDetails[txDetails.size()]);
  }

  static Wallet.TransactionDetails[] getTransfers(String address, Configuration config) {
    // Get latest block
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumRpc.eth_blockNumber()));

    LinkedList<Wallet.TransactionDetails> txDetails = new LinkedList<>();
    Map<String, Object> filterParams = new HashMap<>();
    filterParams.put("fromBlock", "0x0");
    filterParams.put("toBlock", "latest");
    filterParams.put("address", "0x" + config.getStorageContractAddress());
    LinkedList<String> functionTopics = new LinkedList<>();
    functionTopics.add("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef");
    functionTopics.add("0x5548c837ab068cf56a2c2479df0882a4922fd203edb7517321831d95078c5f62");
    for (String functionTopic : functionTopics) {
      Object[] topicArray = new Object[1];
      String[] senderTopic = {functionTopic};
      topicArray[0] = senderTopic;
      filterParams.put("topics", topicArray);
      LOGGER.debug("Requesting filter for: " + Json.stringifyObject(Map.class, filterParams));
      String txFilter = ethereumRpc.eth_newFilter(filterParams);
      LOGGER.debug("Setup filter: " + txFilter);
      Map<String, Object>[] filterResults;
      try {
        LOGGER.debug("Getting filter results...");
        filterResults = ethereumRpc.eth_getFilterLogs(txFilter);
      } catch (Exception e) {
        LOGGER.debug("Something went wrong", e);
        filterResults = new Map[0];
      } finally {
        ethereumRpc.eth_uninstallFilter(txFilter);
      }
      for (Map<String, Object> result : filterResults) {
        LOGGER.debug(result.toString());
        Wallet.TransactionDetails txDetail = new Wallet.TransactionDetails();
        txDetail.setTxHash((String) result.get("transactionHash"));
        try {
          Block block = ethereumRpc.eth_getBlockByNumber((String) result.get("blockNumber"), true);
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

          if (!topics.get(0).equalsIgnoreCase(functionTopic)) {
            continue;
          }

          String from = ByteUtilities.toHexString(
              ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(1))));
          txDetail.setFromAddress(new String[]{from});

          String to = ByteUtilities.toHexString(
              ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(topics.get(2))));
          txDetail.setToAddress(new String[]{to});

          String amount = ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(
              ByteUtilities
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
    }

    LOGGER.debug(Json.stringifyObject(LinkedList.class, txDetails));
    Collections.sort(txDetails, new Filters.TxDateComparator());
    return txDetails.toArray(new Wallet.TransactionDetails[txDetails.size()]);
  }
}
