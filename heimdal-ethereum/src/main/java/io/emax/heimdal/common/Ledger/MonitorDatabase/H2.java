package io.emax.heimdal.common.Ledger.MonitorDatabase;

import io.emax.heimdal.common.Balance;
import io.emax.heimdal.common.BalanceConfirmation;
import io.emax.heimdal.common.Ledger.Monitor;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.SQL;
import org.jooq.lambda.Unchecked;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * An in memory database for storing monitored account data
 */
public class H2 implements Monitor, AutoCloseable {

  public final String currency;
  public final String databaseURL;
  // c3p0 for connection pooling
  private final ComboPooledDataSource c3p0 = new ComboPooledDataSource();

  /**
   * Initializes an H2 monitor database
   *
   * @param currency The currency the database is being used to monitor
   * @throws PropertyVetoException
   * @throws SQLException
   */
  public H2(String currency) throws PropertyVetoException, SQLException {
    this.currency = currency;

    // DB_CLOSE_DELAY=-1 means database is persistent
    // IGNORECASE=TRUE means case is ignored
    // See init.sql in the resources directory to see how this database is initialized
    databaseURL =
        "jdbc:h2:mem:"
            + currency
            + ";DB_CLOSE_DELAY=-1;IGNORECASE=TRUE;INIT=RUNSCRIPT FROM 'classpath:emax/io/heimdal/common/Ledger/MonitorDatabase/init.sql'";
    c3p0.setDriverClass("org.h2.Driver");
    c3p0.setJdbcUrl(databaseURL);
  }

  private Connection getConnection() throws SQLException {
    return c3p0.getConnection();
  }

  @Override
  public String getCurrency() {
    return currency;
  }

  /**
   * Takes a list of hexadecimal accounts and normalizes them to be lower case and not have a
   * leading '0x'
   *
   * @param accounts Account strings in hex
   * @return A stream of accounts with formatting fixed
   * @throws Exception If one of the accounts is not hex, an exception is raised
   */
  private Stream<String> normalizedAccountStream(@NotNull List<String> accounts) throws Exception {
    final List<String> badAccounts =
        accounts.stream().map(String::toLowerCase).filter(s -> !s.matches("^(0x)?[0-9a-f]+$"))
            .limit(1).collect(Collectors.toList());

    if (!badAccounts.isEmpty())
      throw new Exception("Account is not hexadecimal: " + badAccounts.get(0));

    return accounts.stream().map(String::toLowerCase).map(s -> s.replace("0x", ""));
  }


  @Override
  public List<Balance> monitor(@NotNull List<String> accounts) throws Exception {

    try (Connection connection = getConnection()) {
      final String query = "MERGE INTO Accounts (account) VALUES (?)";
      final PreparedStatement statement = connection.prepareStatement(query);
      normalizedAccountStream(accounts).forEach(a -> {
        try {
          statement.setString(1, a);
          statement.addBatch();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      });

      for (int i : statement.executeBatch()) {
        if (i == java.sql.Statement.EXECUTE_FAILED)
          throw new Exception("SQL failed to add account for monitoring: " + accounts.get(i));
      }
    }

    return getBalances(accounts);
  }

  private BalanceConfirmation addBalanceConfirmation(
      @NotNull BalanceConfirmation balanceConfirmation) throws Exception {

    try (Connection connection = getConnection()) {
      final PreparedStatement statement =
          connection
              .prepareStatement("INSERT INTO BalanceConfirmations (BLOCKNUMBER, TIMESTAMP, BLOCKHASH) VALUES (?, ?, ?)");

      statement.setInt(1, (int) balanceConfirmation.getBlockNumber());
      statement.setInt(2, (int) balanceConfirmation.getTimestamp());
      statement.setString(3, balanceConfirmation.getBlockHash());
      statement.executeUpdate();

      final PreparedStatement query =
          connection.prepareStatement("SELECT * from BalanceConfirmations WHERE BLOCKNUMBER = ?");
      query.setInt(1, (int) balanceConfirmation.getBlockNumber());

      List<BalanceConfirmation> returnValues = SQL.seq(query, Unchecked.function(r -> {
        final Long blockNumber = r.getLong("BLOCKNUMBER");
        final Long timestamp = r.getLong("TIMESTAMP");
        String blockHash = r.getString("BLOCKHASH");
        if (r.wasNull())
          blockHash = null;
        return new BalanceConfirmation(blockNumber, timestamp, blockHash);
      })).stream().collect(Collectors.toList());

      if (returnValues.size() != 1)
        throw new Exception("Table should contain just:\n\n" + balanceConfirmation
            + "\n\nBut instead is:\n\n" + returnValues);

      return returnValues.get(0);
    }
  }

  public List<Balance> updateAccounts(@NotNull BalanceConfirmation balanceConfirmation,
      @NotNull List<Balance> balances) throws Exception {
    final ArrayList<String> accounts = new ArrayList<>();
    for (Balance b : balances)
      accounts.add(b.account);
    final List<Balance> oldBalances =
        getBalances(normalizedAccountStream(accounts).collect(Collectors.toList()));
    final Map<String, Balance> balanceMap =
        oldBalances.stream().collect(Collectors.toMap(b -> b.account, Function.identity()));
    balances.forEach(updatedBalance -> {
      if (balanceMap.containsKey(updatedBalance.account)) {
        final Balance oldBalance = balanceMap.get(updatedBalance.account);
        balanceMap.put(updatedBalance.account, new Balance(oldBalance.account,
            updatedBalance.amount, currency,
            oldBalance.firstConfirmation != null ? oldBalance.firstConfirmation
                : balanceConfirmation, balanceConfirmation, Boolean.TRUE));
      } else
        balanceMap.put(updatedBalance.account,
            new Balance(updatedBalance.account, updatedBalance.amount, currency,
                balanceConfirmation, balanceConfirmation, Boolean.TRUE));
    });
    List<Balance> updateDatedBalances = new ArrayList<>(balanceMap.values());
    addBalanceConfirmation(balanceConfirmation);

    try (Connection connection = getConnection()) {
      final String query =
          "MERGE INTO Accounts (account, amount, firstConfirmation, lastConfirmation) VALUES (?, ?, ?, ?)";
      final PreparedStatement statement = connection.prepareStatement(query);
      updateDatedBalances.stream().forEach(a -> {
        try {
          statement.setString(1, a.account);
          statement.setString(2, a.amount);
          statement.setLong(3, a.firstConfirmation.getBlockNumber());
          statement.setLong(4, a.lastConfirmation.getBlockNumber());
          statement.addBatch();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      });

      for (int i : statement.executeBatch())
        if (i == Statement.EXECUTE_FAILED)
          throw new Exception("SQL failed to update balance: " + updateDatedBalances.get(i));
    }

    return getBalances(accounts);
  }

  @Override
  public List<Balance> getBalances() throws Exception {
    try (Connection connection = getConnection()) {
      final PreparedStatement statement = connection.prepareStatement("SELECT * FROM Balances");
      return SQL.seq(statement, Unchecked.function(r -> resultToBalance(r))).stream()
          .collect(Collectors.toList());
    }
  }

  @Override
  public List<Balance> getBalances(@NotNull List<String> accounts) throws Exception {
    try (Connection connection = getConnection()) {

      final String sqlQuery =
          "SELECT * FROM Balances WHERE account IN "
              + "("
              + Collections.nCopies(accounts.size(), "?").stream()
                  .collect(Collectors.joining(", ")) + ")";

      final PreparedStatement statement = connection.prepareStatement(sqlQuery);
      final List<String> normalizedAccounts =
          normalizedAccountStream(accounts).collect(Collectors.toList());
      IntStream.range(0, normalizedAccounts.size()).forEach(idx -> {
        try {
          statement.setString(idx + 1, normalizedAccounts.get(idx));
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      });

      return SQL.seq(statement, Unchecked.function(r -> resultToBalance(r))).stream()
          .collect(Collectors.toList());
    }
  }

  private Balance resultToBalance(ResultSet rs) throws Exception {
    BalanceConfirmation firstConfirmation = null, lastConfirmation = null;

    final Long firstConfirmationBlockNumber = rs.getLong("FIRSTCONFIRMATIONBLOCKNUMBER");
    if (!rs.wasNull()) {
      final Long firstConfirmationTimeStamp = rs.getLong("FIRSTCONFIRMATIONTIMESTAMP");
      String firstConfirmationBlockhash = rs.getString("FIRSTCONFIRMATIONBLOCKHASH");
      if (rs.wasNull())
        firstConfirmationBlockhash = null;
      firstConfirmation =
          new BalanceConfirmation(firstConfirmationTimeStamp, firstConfirmationBlockNumber,
              firstConfirmationBlockhash);
    }

    final Long lastConfirmationBlockNumber = rs.getLong("LASTCONFIRMATIONBLOCKNUMBER");

    if (!rs.wasNull()) {
      final Long lastConfirmationTimeStamp = rs.getLong("LASTCONFIRMATIONTIMESTAMP");
      String lastConfirmationBlockhash = rs.getString("LASTCONFIRMATIONBLOCKHASH");
      if (rs.wasNull())
        lastConfirmationBlockhash = null;
      lastConfirmation =
          new BalanceConfirmation(lastConfirmationTimeStamp, lastConfirmationBlockNumber,
              lastConfirmationBlockhash);
    }

    final String account = rs.getString("ACCOUNT");
    String amount = rs.getString("AMOUNT");

    if (rs.wasNull())
      amount = null;

    return new Balance(account, amount, getCurrency(), firstConfirmation, lastConfirmation,
        Boolean.TRUE);
  }

  public void close() {
    c3p0.close();
  }
}
