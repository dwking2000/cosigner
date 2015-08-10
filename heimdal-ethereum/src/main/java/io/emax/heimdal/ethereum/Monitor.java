package io.emax.heimdal.ethereum;

import java.util.Map;

import rx.Observable;

public class Monitor implements io.emax.heimdal.api.currency.Monitor {

  @Override
  public void addAddresses(Iterable<String> addresses) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeAddresses(Iterable<String> addresses) {
    // TODO Auto-generated method stub

  }

  @Override
  public Iterable<String> listAddresses() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, String> getBalances() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Observable<Map<String, String>> getObservableBalances() {
    // TODO Auto-generated method stub
    return null;
  }

}
