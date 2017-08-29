package io.emax.cosigner.ethereum.tokenstorage.currencyconfigurations;

import io.emax.cosigner.api.core.CurrencyPackageInterface;
import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.ethereum.tokenstorage.Configuration;
import io.emax.cosigner.ethereum.tokenstorage.Monitor;
import io.emax.cosigner.ethereum.tokenstorage.Wallet;

public class GenericCurrencyPackage implements CurrencyPackageInterface {
  private Configuration configuration = null;
  private Wallet wallet = null;
  private Monitor monitor = null;

  public GenericCurrencyPackage(String configurationSymbol) {
    configuration = new Configuration(configurationSymbol);
  }

  public CurrencyConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(CurrencyConfiguration configuration) {
    if (configuration.getClass().isInstance(Configuration.class)) {
      this.configuration = (Configuration) configuration;
    }
  }

  public io.emax.cosigner.api.currency.Wallet getWallet() {
    if (wallet == null) {
      wallet = new Wallet(configuration);
    }
    return wallet;
  }

  public void setWallet(io.emax.cosigner.api.currency.Wallet wallet) {
    if (wallet.getClass().isInstance(Wallet.class)) {
      this.wallet = (Wallet) wallet;
    }
  }

  public io.emax.cosigner.api.currency.Monitor getMonitor() {
    if (monitor == null) {
      monitor = new Monitor((Wallet) getWallet());
    }
    return monitor;
  }

  public void setMonitor(io.emax.cosigner.api.currency.Monitor monitor) {
    if (monitor.getClass().isInstance(Monitor.class)) {
      this.monitor = (Monitor) monitor;
    }
  }
}
