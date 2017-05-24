package io.emax.cosigner.ethereum.core;

import io.emax.cosigner.api.core.CurrencyPackageInterface;
import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.Monitor;
import io.emax.cosigner.api.currency.Wallet;

public class EthereumCurrencyPackage implements CurrencyPackageInterface {
  private EthereumConfiguration ethereumConfiguration = new EthereumConfiguration();
  private EthereumWallet ethereumWallet = null;
  private EthereumMonitor ethereumMonitor = null;

  public CurrencyConfiguration getConfiguration() {
    return ethereumConfiguration;
  }

  public void setConfiguration(CurrencyConfiguration configuration) {
    if (configuration.getClass().isInstance(EthereumConfiguration.class)) {
      ethereumConfiguration = (EthereumConfiguration) configuration;
    }
  }

  public Wallet getWallet() {
    if (ethereumWallet == null) {
      ethereumWallet = new EthereumWallet(ethereumConfiguration);
    }
    return ethereumWallet;
  }

  public void setWallet(Wallet wallet) {
    if (wallet.getClass().isInstance(EthereumWallet.class)) {
      ethereumWallet = (EthereumWallet) wallet;
    }
  }

  public Monitor getMonitor() {
    if (ethereumMonitor == null) {
      ethereumMonitor = new EthereumMonitor((EthereumWallet) getWallet());
    }
    return ethereumMonitor;
  }

  public void setMonitor(Monitor monitor) {
    if (monitor.getClass().isInstance(EthereumMonitor.class)) {
      ethereumMonitor = (EthereumMonitor) monitor;
    }
  }
}
