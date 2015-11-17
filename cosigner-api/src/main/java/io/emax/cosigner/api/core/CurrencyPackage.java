package io.emax.cosigner.api.core;

import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.Monitor;
import io.emax.cosigner.api.currency.Wallet;

public class CurrencyPackage {
  private CurrencyConfiguration configuration;
  private Wallet wallet;
  private Monitor monitor;

  public CurrencyConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(CurrencyConfiguration configuration) {
    this.configuration = configuration;
  }

  public Wallet getWallet() {
    return wallet;
  }

  public void setWallet(Wallet wallet) {
    this.wallet = wallet;
  }

  public Monitor getMonitor() {
    return monitor;
  }

  public void setMonitor(Monitor monitor) {
    this.monitor = monitor;
  }

  @Override
  public String toString() {
    return "CurrencyPackage [configuration=" + configuration + ", wallet=" + wallet + ", monitor="
        + monitor + "]";
  }
}
