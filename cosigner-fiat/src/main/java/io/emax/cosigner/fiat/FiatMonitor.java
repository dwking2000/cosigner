package io.emax.cosigner.fiat;

import io.emax.cosigner.api.currency.Wallet.TransactionDetails;

import rx.Observable;
import rx.Subscription;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class FiatMonitor implements io.emax.cosigner.api.currency.Monitor {
  private final HashSet<String> monitoredAddresses = new HashSet<>();
  private final HashMap<String, String> accountBalances = new HashMap<>();
  private final HashSet<TransactionDetails> accountTransactions = new HashSet<>();
  private final HashSet<TransactionDetails> newAccountTransactions = new HashSet<>();

  private FiatConfiguration config;

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

  private final FiatWallet wallet;

  public FiatMonitor(FiatConfiguration config) {
    this.config = config;
    wallet = new FiatWallet(config);
  }

  public FiatMonitor(FiatWallet inputWallet) {
    wallet = inputWallet;
  }

  private boolean updateBalances() {
    monitoredAddresses.forEach(address -> {
      String currentBalance = wallet.getBalance(address);
      accountBalances.put(address, currentBalance);
    });

    updateTransactions();
    return true;
  }

  private void updateTransactions() {
    HashSet<TransactionDetails> details = new HashSet<>();
    monitoredAddresses.forEach(
        address -> Arrays.asList(wallet.getTransactions(address, 100, 0)).forEach(details::add));

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
    return new FiatMonitor(config);
  }

  @Override
  public void destroyMonitor() {
    if (balanceSubscription != null) {
      balanceSubscription.unsubscribe();
    }
  }
}
