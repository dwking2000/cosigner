package io.emax.cosigner.ethereum.core;

import io.emax.cosigner.api.currency.Wallet.TransactionDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Subscription;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class EthereumMonitor implements io.emax.cosigner.api.currency.Monitor {
  private static final Logger LOGGER = LoggerFactory.getLogger(EthereumMonitor.class);
  private final HashSet<String> monitoredAddresses = new HashSet<>();
  private final HashMap<String, String> accountBalances = new HashMap<>();
  private final HashSet<TransactionDetails> accountTransactions = new HashSet<>();
  private final HashSet<TransactionDetails> newAccountTransactions = new HashSet<>();

  private final Observable<Map<String, String>> observableBalances =
      Observable.interval(1, TimeUnit.MINUTES).map(tick -> accountBalances);

  private final Observable<Set<TransactionDetails>> observableTransactions =
      Observable.interval(1, TimeUnit.MINUTES).map(tick -> {
        HashSet<TransactionDetails> txs = new HashSet<>();
        txs.addAll(newAccountTransactions);
        newAccountTransactions.clear();
        return txs;
      });

  private final Subscription balanceSubscription =
      Observable.interval(30, TimeUnit.SECONDS).map(tick -> updateBalances()).subscribe();

  private final EthereumWallet wallet;
  private final EthereumConfiguration config;

  public EthereumMonitor(EthereumConfiguration conf) {
    config = conf;
    wallet = new EthereumWallet(conf);
  }

  public EthereumMonitor(EthereumWallet inputWallet) {
    wallet = inputWallet;
    config = wallet.config;
  }

  private boolean updateBalances() {
    LOGGER.debug("ETH: Checking balances....");
    monitoredAddresses.forEach(address -> {
      try {
        LOGGER.debug("ETH: Checking balance for: " + address);
        String currentBalance = wallet.getBalance(address);
        accountBalances.put(address, currentBalance);
      } catch (Exception e) {
        LOGGER.debug(null, e);
      }
    });

    updateTransactions();
    return true;
  }

  private void updateTransactions() {
    HashSet<TransactionDetails> details = new HashSet<>();
    monitoredAddresses.forEach(address -> {
      try {
        Arrays.asList(wallet.getTransactions(address, 100, 0)).forEach(details::add);
      } catch (Exception e) {
        LOGGER.error("Problem updating monitored transactions", e);
      }
    });

    // Remove the intersection
    details.removeAll(accountTransactions);
    accountTransactions.addAll(details);
    newAccountTransactions.addAll(details);

  }

  @Override
  public void addAddresses(Iterable<String> addresses) {
    addresses.forEach(monitoredAddresses::add);
  }

  @Override
  public void removeAddresses(Iterable<String> addresses) {
    addresses.forEach(monitoredAddresses::remove);
  }

  @Override
  public Iterable<String> listAddresses() {
    LinkedList<String> addresses = new LinkedList<>();
    monitoredAddresses.forEach(addresses::add);
    return addresses;
  }

  @Override
  public Map<String, String> getBalances() {
    return accountBalances;
  }

  @Override
  public Observable<Map<String, String>> getObservableBalances() {
    return observableBalances;
  }

  @Override
  public Set<TransactionDetails> getTransactions() {
    return accountTransactions;
  }

  @Override
  public Observable<Set<TransactionDetails>> getObservableTransactions() {
    return observableTransactions;
  }

  @Override
  public io.emax.cosigner.api.currency.Monitor createNewMonitor() {
    return new EthereumMonitor(config);
  }

  @Override
  public void destroyMonitor() {
    if (balanceSubscription != null) {
      balanceSubscription.unsubscribe();
    }
  }
}
