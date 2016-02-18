package io.emax.cosigner.fiat;

import io.emax.cosigner.api.currency.Monitor;
import io.emax.cosigner.api.currency.Wallet;

import rx.Observable;

import java.util.Map;
import java.util.Set;

public class FiatMonitor implements Monitor {
  @Override
  public Monitor createNewMonitor() {
    return null;
  }

  @Override
  public void destroyMonitor() {

  }

  @Override
  public void addAddresses(Iterable<String> addresses) {

  }

  @Override
  public void removeAddresses(Iterable<String> addresses) {

  }

  @Override
  public Iterable<String> listAddresses() {
    return null;
  }

  @Override
  public Map<String, String> getBalances() {
    return null;
  }

  @Override
  public Observable<Map<String, String>> getObservableBalances() {
    return null;
  }

  @Override
  public Set<Wallet.TransactionDetails> getTransactions() {
    return null;
  }

  @Override
  public Observable<Set<Wallet.TransactionDetails>> getObservableTransactions() {
    return null;
  }
}
