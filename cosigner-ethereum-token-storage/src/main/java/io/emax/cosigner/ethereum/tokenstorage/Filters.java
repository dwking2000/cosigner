package io.emax.cosigner.ethereum.tokenstorage;

import com.google.common.primitives.UnsignedLong;

import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.ethereum.core.gethrpc.Block;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

  private static HashMap<String, Configuration> scanners = new HashMap<>();
  private static final Object scannerLock = new Object();
  private static ExecutorService scanner = null;
  private static long maxBlocksToScan = 5000;
  private static Connection txDb = null;

  private static void filterScanner() {
    //noinspection InfiniteLoopStatement
    while (true) {
      try {
        // Copy of the map for thread safety.
        HashMap<String, Configuration> scannerMap = new HashMap<>();
        synchronized (scannerLock) {
          scanners.forEach(scannerMap::put);
        }

        for (Configuration scannerConfig : scannerMap.values()) {
          scanReconciliations(scannerConfig);
          Thread.sleep(500); // Yield between scans so that we don't pin the CPU all the time.
          scanTransfers(scannerConfig);
          Thread.sleep(500);
        }
      } catch (Exception e) {
        LOGGER.debug("Problem in token storage tx scanner", e);
      }
    }
  }

  public static void addScanner(Configuration config) {
    synchronized (scannerLock) {
      scanners.put(config.getCurrencySymbol(), config);

      try {
        if (txDb == null) {
          Class.forName("org.h2.Driver");
          txDb = DriverManager.
              getConnection("jdbc:h2:mem:txs;mode=MySQL");

          Statement stmt = txDb.createStatement();
          String query =
              "CREATE TABLE IF NOT EXISTS TXS(address char(64) not null, topic char(128) not null, txhash char(128) not null, blocknumber bigint not null, txdata other not null);";
          stmt.execute(query);
          query = "ALTER TABLE TXS ADD PRIMARY KEY (address, topic, txhash);";
          stmt.execute(query);
          stmt.close();
        }

        if (scanner == null) {
          scanner = Executors.newFixedThreadPool(1);
          scanner.execute(Filters::filterScanner);
        }
      } catch (Exception e) {
        LOGGER.error("Error initializing TX Database or Scanner", e);
      }
    }
  }

  static HashMap<String, Long> lastReconciliationBlock = new HashMap<>();

  private static void scanReconciliations(Configuration config) throws Exception {
    // Get latest block
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumReadRpc.eth_blockNumber()));

    Map<String, Object> filterParams = new HashMap<>();
    filterParams.put("address", "0x" + config.getStorageContractAddress());
    LinkedList<String> functionTopics = new LinkedList<>();
    functionTopics.add("0x73bb00f3ad09ef6bc524e5cf56563dff2bc6663caa0b4054aa5946811083ed2e");
    @SuppressWarnings("unchecked") Map<String, Object>[] filterResults = new Map[0];
    for (String functionTopic : functionTopics) {
      synchronized (scannerLock) {
        if (!lastReconciliationBlock
            .containsKey(config.getStorageContractAddress() + functionTopic)) {
          String query = "SELECT MAX(blocknumber) FROM TXS WHERE address = ? AND topic = ?;";
          PreparedStatement stmt = txDb.prepareStatement(query);
          stmt.setString(1, config.getStorageContractAddress());
          stmt.setString(2, functionTopic);
          ResultSet rs = stmt.executeQuery();

          if (rs.next()) {
            lastReconciliationBlock.put(config.getStorageContractAddress() + functionTopic,
                Math.max(0L, rs.getLong(1) - (config.getMinConfirmations() * 2)));
          } else {
            lastReconciliationBlock.put(config.getStorageContractAddress() + functionTopic, 0L);
          }
          rs.close();
          stmt.close();
        }
        String startBlock = "0x" + Long.toHexString(Math.max(0L,
            lastReconciliationBlock.get(config.getStorageContractAddress() + functionTopic) - (2
                * config.getMinConfirmations())));
        String endBlock = "0x" + Long.toHexString(Math.min(latestBlockNumber.longValue(),
            lastReconciliationBlock.get(config.getStorageContractAddress() + functionTopic)
                + maxBlocksToScan));
        filterParams.put("fromBlock", startBlock);
        filterParams.put("toBlock", endBlock);

        if (startBlock.equalsIgnoreCase(endBlock)) {
          continue;
        }
      }
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
          //noinspection unchecked
          filterResults = new Map[0];
        }
      } catch (Exception e) {
        LOGGER.debug("Something went wrong", e);
        //noinspection unchecked
        filterResults = new Map[0];
        filterSucceeded = false;
      } finally {
        if (filterSucceeded) {
          synchronized (scannerLock) {
            long newLastBlock =
                lastReconciliationBlock.get(config.getStorageContractAddress() + functionTopic)
                    + maxBlocksToScan;
            lastReconciliationBlock.put(config.getStorageContractAddress() + functionTopic,
                Math.min(newLastBlock, latestBlockNumber.longValue()));

            Arrays.asList(filterResults).forEach(result -> {
              try {
                String query =
                    "INSERT INTO TXS VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE blocknumber = ?, txdata = ?;";
                PreparedStatement stmt = txDb.prepareStatement(query);
                stmt.setString(1, config.getStorageContractAddress());
                stmt.setString(2, functionTopic);
                stmt.setString(3, (String) result.get("transactionHash"));
                stmt.setLong(4,
                    UnsignedLong.valueOf(((String) result.get("blockNumber")).substring(2), 16)
                        .longValue());
                stmt.setObject(5, result);
                stmt.setLong(6,
                    UnsignedLong.valueOf(((String) result.get("blockNumber")).substring(2), 16)
                        .longValue());
                stmt.setObject(7, result);
                stmt.executeUpdate();
                stmt.close();
              } catch (Exception e) {
                LOGGER.error("Problem caching transaction!", e);
              }
            });
          }
        }
        ethereumWriteRpc.eth_uninstallFilter(txFilter);
      }
    }
  }

  static Wallet.TransactionDetails[] getReconciliations(String address, Configuration config)
      throws Exception {
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumReadRpc.eth_blockNumber()));
    LinkedList<Wallet.TransactionDetails> txDetails = new LinkedList<>();

    @SuppressWarnings("unchecked") Map<String, Object>[] filterResults = new Map[0];


    LinkedList<String> functionTopics = new LinkedList<>();
    functionTopics.add("0x73bb00f3ad09ef6bc524e5cf56563dff2bc6663caa0b4054aa5946811083ed2e");
    for (String functionTopic : functionTopics) {
      synchronized (scannerLock) {
        LinkedList<Map<String, Object>> queriedTxs = new LinkedList<>();
        String query = "SELECT txdata from TXS where address = ? and topic = ?;";
        PreparedStatement stmt = txDb.prepareStatement(query);
        stmt.setString(1, config.getStorageContractAddress());
        stmt.setString(2, functionTopic);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
          //noinspection unchecked
          queriedTxs.add((Map<String, Object>) rs.getObject(1));
        }

        rs.close();
        stmt.close();

        filterResults = queriedTxs.toArray(filterResults);
      }
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

          @SuppressWarnings("unchecked") ArrayList<String> topics =
              (ArrayList<String>) result.get("topics");

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
    txDetails.sort(new TxDateComparator());
    return txDetails.toArray(new Wallet.TransactionDetails[txDetails.size()]);
  }

  static HashMap<String, Long> lastTransferBlock = new HashMap<>();

  private static void scanTransfers(Configuration config) throws Exception {
    // Get latest block
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumReadRpc.eth_blockNumber()));

    Map<String, Object> filterParams = new HashMap<>();
    filterParams.put("address", "0x" + config.getStorageContractAddress());
    LinkedList<String> functionTopics = new LinkedList<>();
    functionTopics.add("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef");
    functionTopics.add("0x5548c837ab068cf56a2c2479df0882a4922fd203edb7517321831d95078c5f62");
    @SuppressWarnings("unchecked") Map<String, Object>[] filterResults = new Map[0];

    for (String functionTopic : functionTopics) {
      synchronized (scannerLock) {
        String query = "SELECT MAX(blocknumber) FROM TXS WHERE address = ? AND topic = ?;";
        PreparedStatement stmt = txDb.prepareStatement(query);
        stmt.setString(1, config.getStorageContractAddress());
        stmt.setString(2, functionTopic);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
          lastTransferBlock.put(config.getStorageContractAddress() + functionTopic,
              Math.max(0L, rs.getLong(1) - (config.getMinConfirmations() * 2)));
        } else {
          lastTransferBlock.put(config.getStorageContractAddress() + functionTopic, 0L);
        }
        rs.close();
        stmt.close();
        String startBlock = "0x" + Long.toHexString(Math.max(0L,
            lastTransferBlock.get(config.getStorageContractAddress() + functionTopic) - (2 * config
                .getMinConfirmations())));
        String endBlock = "0x" + Long.toHexString(Math.min(latestBlockNumber.longValue(),
            lastTransferBlock.get(config.getStorageContractAddress() + functionTopic)
                + maxBlocksToScan));
        filterParams.put("fromBlock", startBlock);
        filterParams.put("toBlock", endBlock);

        if (startBlock.equalsIgnoreCase(endBlock)) {
          continue;
        }
      }
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
          //noinspection unchecked
          filterResults = new Map[0];
        }
      } catch (Exception e) {
        LOGGER.debug("Something went wrong", e);
        //noinspection unchecked
        filterResults = new Map[0];
        filterSucceeded = false;
      } finally {
        if (filterSucceeded) {
          synchronized (scannerLock) {
            long newLastBlock =
                lastTransferBlock.get(config.getStorageContractAddress() + functionTopic)
                    + maxBlocksToScan;
            lastTransferBlock.put(config.getStorageContractAddress() + functionTopic,
                Math.min(newLastBlock, latestBlockNumber.longValue()));

            Arrays.asList(filterResults).forEach(result -> {
              try {
                String query =
                    "INSERT INTO TXS VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE blocknumber = ?, txdata = ?;";
                PreparedStatement stmt = txDb.prepareStatement(query);
                stmt.setString(1, config.getStorageContractAddress());
                stmt.setString(2, functionTopic);
                stmt.setString(3, (String) result.get("transactionHash"));
                stmt.setLong(4,
                    UnsignedLong.valueOf(((String) result.get("blockNumber")).substring(2), 16)
                        .longValue());
                stmt.setObject(5, result);
                stmt.setLong(6,
                    UnsignedLong.valueOf(((String) result.get("blockNumber")).substring(2), 16)
                        .longValue());
                stmt.setObject(7, result);
                stmt.executeUpdate();
                stmt.close();
              } catch (Exception e) {
                LOGGER.error("Problem caching transaction!", e);
              }
            });
          }
        }
        ethereumWriteRpc.eth_uninstallFilter(txFilter);
      }
    }
  }

  static Wallet.TransactionDetails[] getTransfers(String address, Configuration config)
      throws Exception {
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumReadRpc.eth_blockNumber()));
    LinkedList<Wallet.TransactionDetails> txDetails = new LinkedList<>();

    LinkedList<String> functionTopics = new LinkedList<>();
    functionTopics.add("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef");
    functionTopics.add("0x5548c837ab068cf56a2c2479df0882a4922fd203edb7517321831d95078c5f62");

    @SuppressWarnings("unchecked") Map<String, Object>[] filterResults = new Map[0];
    for (String functionTopic : functionTopics) {
      synchronized (scannerLock) {
        LinkedList<Map<String, Object>> queriedTxs = new LinkedList<>();
        String query = "SELECT txdata from TXS where address = ? and topic = ?;";
        PreparedStatement stmt = txDb.prepareStatement(query);
        stmt.setString(1, config.getStorageContractAddress());
        stmt.setString(2, functionTopic);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
          //noinspection unchecked
          queriedTxs.add((Map<String, Object>) rs.getObject(1));
        }

        rs.close();
        stmt.close();

        filterResults = queriedTxs.toArray(filterResults);
      }
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

          @SuppressWarnings("unchecked") ArrayList<String> topics =
              (ArrayList<String>) result.get("topics");

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
    txDetails.sort(new TxDateComparator());
    return txDetails.toArray(new Wallet.TransactionDetails[txDetails.size()]);
  }
}
