package io.emax.cosigner.api.core;

import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.Monitor;
import io.emax.cosigner.api.currency.Wallet;

public interface CurrencyPackageInterface {
  CurrencyConfiguration getConfiguration();

  void setConfiguration(CurrencyConfiguration configuration);

  Wallet getWallet();

  void setWallet(Wallet wallet);

  Monitor getMonitor();

  void setMonitor(Monitor monitor);
}
