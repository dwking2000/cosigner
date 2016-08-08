package io.emax.cosigner.fiat.CurrencyConfigurations;

import io.emax.cosigner.api.core.CurrencyPackageInterface;
import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.Monitor;
import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.fiat.FiatConfiguration;
import io.emax.cosigner.fiat.FiatMonitor;
import io.emax.cosigner.fiat.FiatWallet;

public class EuroCurrencyPackage implements CurrencyPackageInterface {
  private FiatConfiguration fiatConfiguration = new FiatConfiguration("EUR");
  private FiatWallet fiatWallet = null;
  private FiatMonitor fiatMonitor = null;

  @Override
  public CurrencyConfiguration getConfiguration() {
    return fiatConfiguration;
  }

  @Override
  public void setConfiguration(CurrencyConfiguration configuration) {
    if(configuration.getClass().isInstance(FiatConfiguration.class)) {
      fiatConfiguration = (FiatConfiguration) configuration;
    }
  }

  @Override
  public Wallet getWallet() {
    if(fiatWallet == null) {
      fiatWallet = new FiatWallet(fiatConfiguration);
    }
    return fiatWallet;
  }

  @Override
  public void setWallet(Wallet wallet) {
    if(wallet.getClass().isInstance(FiatWallet.class)) {
      fiatWallet = (FiatWallet) wallet;
    }
  }

  @Override
  public Monitor getMonitor() {
    if(fiatMonitor == null) {
      fiatMonitor = new FiatMonitor((FiatWallet) getWallet());
    }
    return fiatMonitor;
  }

  @Override
  public void setMonitor(Monitor monitor) {
    if(monitor.getClass().isInstance(FiatMonitor.class)) {
      fiatMonitor = (FiatMonitor) monitor;
    }
  }
}
