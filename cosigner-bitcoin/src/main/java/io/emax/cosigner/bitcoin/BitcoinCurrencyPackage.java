package io.emax.cosigner.bitcoin;

import io.emax.cosigner.api.core.CurrencyPackageInterface;
import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.Monitor;
import io.emax.cosigner.api.currency.Wallet;

public class BitcoinCurrencyPackage implements CurrencyPackageInterface {
  private BitcoinConfiguration bitcoinConfiguration = new BitcoinConfiguration();
  private BitcoinWallet bitcoinWallet = null;
  private BitcoinMonitor bitcoinMonitor = null;

  @Override
  public CurrencyConfiguration getConfiguration() {
    return bitcoinConfiguration;
  }

  @Override
  public void setConfiguration(CurrencyConfiguration configuration) {
    if (configuration.getClass().isInstance(BitcoinConfiguration.class)) {
      bitcoinConfiguration = (BitcoinConfiguration) configuration;
    }
  }

  @Override
  public Wallet getWallet() {
    if (bitcoinWallet == null) {
      bitcoinWallet = new BitcoinWallet(bitcoinConfiguration);
    }
    return bitcoinWallet;
  }

  @Override
  public void setWallet(Wallet wallet) {
    if (wallet.getClass().isInstance(BitcoinWallet.class)) {
      bitcoinWallet = (BitcoinWallet) wallet;
    }
  }

  @Override
  public Monitor getMonitor() {
    if (bitcoinMonitor == null) {
      bitcoinMonitor = new BitcoinMonitor((BitcoinWallet) getWallet());
    }
    return bitcoinMonitor;

  }

  @Override
  public void setMonitor(Monitor monitor) {
    if (monitor.getClass().isInstance(BitcoinMonitor.class)) {
      bitcoinMonitor = (BitcoinMonitor) monitor;
    }
  }
}
