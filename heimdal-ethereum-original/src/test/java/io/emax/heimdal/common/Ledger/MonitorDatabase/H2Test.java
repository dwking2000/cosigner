package io.emax.heimdal.common.Ledger.MonitorDatabase;

import io.emax.heimdal.common.Balance;
import io.emax.heimdal.common.BalanceConfirmation;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class H2Test {

  static {
    try (H2 testDB = new H2("TEST")) {
      ComboPooledDataSource c3p0 = new ComboPooledDataSource();
      c3p0.setDriverClass("org.h2.Driver");
      c3p0.setJdbcUrl(testDB.databaseURL);
      try (Connection connection = c3p0.getConnection()) {
        try (Statement statement = connection.createStatement()) {
          statement.execute("MERGE INTO BalanceConfirmations VALUES (1, 1, 'ABC')");
          statement.execute("MERGE INTO BalanceConfirmations VALUES (2, 2, 'ABCD')");
          statement.execute("MERGE INTO Accounts VALUES ('56abc', '9999', 1, 2)");
          statement.execute("MERGE INTO Accounts VALUES ('55aaa', '8888', null, null)");
        }
      }
    } catch (SQLException | PropertyVetoException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testGetCurrency() throws Exception {
    try (H2 testDB = new H2("TEST")) {
      Assert.assertEquals("TEST", testDB.getCurrency());
    }
  }

  @Test
  public void testGetBalances() throws Exception {

    try (H2 testDB = new H2("TEST")) {
      List<Balance> actualList = testDB.getBalances();
      Balance[] actual = actualList.toArray(new Balance[actualList.size()]);
      String expectedJSON =
          "[{\"currency\":\"TEST\",\"amount\":\"9999\",\"account\":\"56abc\",\"monitored\":true,\"firstConfirmation\":{\"timestamp\":1,\"blockNumber\":1,\"blockHash\":\"ABC\"},\"lastConfirmation\":{\"timestamp\":2,\"blockNumber\":2,\"blockHash\":\"ABCD\"}}, {\"currency\":\"TEST\",\"amount\":\"8888\",\"account\":\"55aaa\",\"monitored\":true}]";
      Balance[] expected = new Gson().fromJson(expectedJSON, Balance[].class);
      Assert.assertArrayEquals(expected, actual);
    }
  }

  @Test
  public void testGetBalancesList() throws Exception {
    try (H2 testDB = new H2("TEST")) {
      List<Balance> actualList = testDB.getBalances(Collections.singletonList("56abc"));
      Balance[] actual = actualList.toArray(new Balance[actualList.size()]);
      String expectedJSON =
          "[{\"currency\":\"TEST\",\"amount\":\"9999\",\"account\":\"56abc\",\"monitored\":true,\"firstConfirmation\":{\"timestamp\":1,\"blockNumber\":1,\"blockHash\":\"ABC\"},\"lastConfirmation\":{\"timestamp\":2,\"blockNumber\":2,\"blockHash\":\"ABCD\"}}]";
      Balance[] expected = new Gson().fromJson(expectedJSON, Balance[].class);
      Assert.assertArrayEquals(expected, actual);
      List<Balance> expected2 = testDB.getBalances();
      expected2.sort((Balance a, Balance b) -> a.account.compareTo(b.account));
      List<Balance> actual2 = testDB.getBalances(Arrays.asList("0x55AAA", "0x56ABC", "55555"));
      actual2.sort((Balance a, Balance b) -> a.account.compareTo(b.account));
      Assert.assertEquals("getBalance() for \"56ABC\" and \"55AAA\" is the same as getBalance()",
          expected2, actual2);
    }
  }

  @Test
  public void testMonitor() throws Exception {
    try (H2 testDB = new H2("TEST")) {
      List<Balance> actualList = testDB.monitor(Arrays.asList("0xDEADBEEF1", "DEADBeeF2"));
      Balance[] actual = actualList.toArray(new Balance[actualList.size()]);
      String expectedJSON =
          "[" + "{\"currency\":\"TEST\",\"account\":\"deadbeef1\",\"monitored\":true},"
              + "{\"currency\":\"TEST\",\"account\":\"deadbeef2\",\"monitored\":true}" + "]";
      Balance[] expected = new Gson().fromJson(expectedJSON, Balance[].class);
      Assert.assertArrayEquals(expected, actual);
    }
  }

  @Test
  public void testMonitorOldAccount() throws Exception {
    try (H2 testDB = new H2("TEST")) {
      List<Balance> actualList = testDB.monitor(Arrays.asList("0xDEADBEEF1", "56abc"));
      actualList.sort((Balance a, Balance b) -> a.account.compareTo(b.account));
      Balance[] actual = actualList.toArray(new Balance[actualList.size()]);
      String expectedJSON =
          "["
              + "{\"currency\":\"TEST\",\"amount\":\"9999\",\"account\":\"56abc\",\"monitored\":true,\"firstConfirmation\":{\"timestamp\":1,\"blockNumber\":1,\"blockHash\":\"ABC\"},\"lastConfirmation\":{\"timestamp\":2,\"blockNumber\":2,\"blockHash\":\"ABCD\"}},"
              + "{\"currency\":\"TEST\",\"account\":\"deadbeef1\",\"monitored\":true}" + "]";
      Balance[] expected = new Gson().fromJson(expectedJSON, Balance[].class);
      Arrays.sort(expected, (Balance a, Balance b) -> a.account.compareTo(b.account));
      Assert.assertArrayEquals(expected, actual);
    }
  }

  @Test
  public void testUpdateAccounts() throws Exception {
    try (H2 testDB = new H2("TEST")) {
      List<Balance> actualList = testDB.getBalances(Arrays.asList("56abc", "faafee"));
      actualList.sort((Balance a, Balance b) -> a.account.compareTo(b.account));
      Balance[] actual = actualList.toArray(new Balance[actualList.size()]);
      String expectedJSON =
          "["
              + "{\"currency\":\"TEST\",\"amount\":\"9999\",\"account\":\"56abc\",\"monitored\":true,\"firstConfirmation\":{\"timestamp\":1,\"blockNumber\":1,\"blockHash\":\"ABC\"},\"lastConfirmation\":{\"timestamp\":2,\"blockNumber\":2,\"blockHash\":\"ABCD\"}}"
              + "]";
      Balance[] expected = new Gson().fromJson(expectedJSON, Balance[].class);
      Assert.assertArrayEquals(expected, actual);

      List<Balance> expected2 =
          Arrays
              .asList(new Gson()
                  .fromJson(
                      "["
                          + "{\"currency\":\"TEST\",\"amount\":\"99999\",\"account\":\"56abc\",\"monitored\":true,\"firstConfirmation\":{\"timestamp\":1,\"blockNumber\":1,\"blockHash\":\"ABC\"},\"lastConfirmation\":{\"timestamp\":3,\"blockNumber\":3,\"blockHash\":\"FFFFAAA\"}}, "
                          + "{\"currency\":\"TEST\",\"amount\":\"99999999\",\"account\":\"faafee\",\"monitored\":true,\"firstConfirmation\":{\"timestamp\":3,\"blockNumber\":3,\"blockHash\":\"FFFFAAA\"},\"lastConfirmation\":{\"timestamp\":3,\"blockNumber\":3,\"blockHash\":\"FFFFAAA\"}}"
                          + "]", Balance[].class));
      Collections.sort(expected2, (Balance a, Balance b) -> a.account.compareTo(b.account));
      List<Balance> balances =
          Arrays
              .asList(new Gson()
                  .fromJson(
                      "["
                          + "{\"currency\":\"TEST\",\"amount\":\"99999\",\"account\":\"56abc\",\"monitored\":true,\"firstConfirmation\":{\"timestamp\":1,\"blockNumber\":1,\"blockHash\":\"ABC\"},\"lastConfirmation\":{\"timestamp\":3,\"blockNumber\":3,\"blockHash\":\"FFFFAAA\"}},"
                          + "{\"currency\":\"TEST\",\"amount\":\"99999999\",\"account\":\"faafee\",\"monitored\":true,\"firstConfirmation\":{\"timestamp\":1,\"blockNumber\":1,\"blockHash\":\"ABC\"},\"lastConfirmation\":{\"timestamp\":2,\"blockNumber\":2,\"blockHash\":\"FFFF\"}}"
                          + "]", Balance[].class));
      BalanceConfirmation balanceConfirmation = balances.get(0).lastConfirmation;
      List<Balance> actual2 = testDB.updateAccounts(balanceConfirmation, balances);
      Collections.sort(actual2, (Balance a, Balance b) -> a.account.compareTo(b.account));
      Assert.assertEquals(expected2, actual2);
    }
  }

}
