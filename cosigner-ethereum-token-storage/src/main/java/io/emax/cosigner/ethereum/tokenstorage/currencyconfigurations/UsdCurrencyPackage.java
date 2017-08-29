package io.emax.cosigner.ethereum.tokenstorage.currencyconfigurations;

import io.emax.cosigner.api.core.CurrencyPackageInterface;
import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.ethereum.tokenstorage.Monitor;
import io.emax.cosigner.ethereum.tokenstorage.Configuration;
import io.emax.cosigner.ethereum.tokenstorage.Wallet;

public class UsdCurrencyPackage implements CurrencyPackageInterface {
  private Configuration configuration = new Configuration("USD");
  private Wallet wallet = null;
  private Monitor monitor = null;

  @Override
  public CurrencyConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public void setConfiguration(CurrencyConfiguration configuration) {
    if(configuration.getClass().isInstance(Configuration.class)) {
      this.configuration = (Configuration) configuration;
    }
  }

  @Override
  public io.emax.cosigner.api.currency.Wallet getWallet() {
    if(wallet == null) {
      wallet = new Wallet(configuration);
    }
    return wallet;
  }

  @Override
  public void setWallet(io.emax.cosigner.api.currency.Wallet wallet) {
    if(wallet.getClass().isInstance(Wallet.class)) {
      this.wallet = (Wallet) wallet;
    }
  }

  @Override
  public io.emax.cosigner.api.currency.Monitor getMonitor() {
    if(monitor == null) {
      monitor = new Monitor((Wallet) getWallet());
    }
    return monitor;
  }

  @Override
  public void setMonitor(io.emax.cosigner.api.currency.Monitor monitor) {
    if(monitor.getClass().isInstance(Monitor.class)) {
      this.monitor = (Monitor) monitor;
    }
  }
}
