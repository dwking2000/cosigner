package io.emax.heimdal.ethereum;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Observable;

public class Monitor implements io.emax.heimdal.api.currency.Monitor {
  private HashSet<String> monitoredAddresses = new HashSet<>();
  private HashMap<String, String> accountBalances = new HashMap<>();
  private Observable<Map<String, String>> observableBalances =
      Observable.interval(1, TimeUnit.MINUTES).map(tick -> accountBalances);
  private Wallet wallet = new Wallet();

  public Monitor() {
    Observable.interval(30, TimeUnit.SECONDS).map(tick -> updateBalances()).subscribe();
  }

  private boolean updateBalances() {
    monitoredAddresses.forEach(address -> {
      String currentBalance = wallet.getBalance(address);
      accountBalances.put(address, currentBalance);
    });
    return true;
  }

  @Override
  public void addAddresses(Iterable<String> addresses) {
    addresses.forEach(address -> monitoredAddresses.add(address));
  }

  @Override
  public void removeAddresses(Iterable<String> addresses) {
    addresses.forEach(monitoredAddresses::remove);
  }

  @Override
  public Iterable<String> listAddresses() {
    LinkedList<String> addresses = new LinkedList<>();
    monitoredAddresses.forEach(address -> addresses.add(address));
    return null;
  }

  @Override
  public Map<String, String> getBalances() {
    return accountBalances;
  }

  @Override
  public Observable<Map<String, String>> getObservableBalances() {
    return observableBalances;
  }
}
