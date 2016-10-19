package io.emax.cosigner.ethereum.token.CurrencyConfigurations;

import io.emax.cosigner.api.core.CurrencyPackageInterface;
import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.Monitor;
import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.ethereum.token.TokenConfiguration;
import io.emax.cosigner.ethereum.token.TokenMonitor;
import io.emax.cosigner.ethereum.token.TokenWallet;

public class GenericCurrencyPackage implements CurrencyPackageInterface {
  private TokenConfiguration tokenConfiguration = null;
  private TokenWallet tokenWallet = null;
  private TokenMonitor tokenMonitor = null;

  public GenericCurrencyPackage(String configurationSymbol) {
    tokenConfiguration = new TokenConfiguration(configurationSymbol);
  }

  public CurrencyConfiguration getConfiguration() {
    return tokenConfiguration;
  }

  public void setConfiguration(CurrencyConfiguration configuration) {
    if (configuration.getClass().isInstance(TokenConfiguration.class)) {
      tokenConfiguration = (TokenConfiguration) configuration;
    }
  }

  public Wallet getWallet() {
    if (tokenWallet == null) {
      tokenWallet = new TokenWallet(tokenConfiguration);
    }
    return tokenWallet;
  }

  public void setWallet(Wallet wallet) {
    if (wallet.getClass().isInstance(TokenWallet.class)) {
      tokenWallet = (TokenWallet) wallet;
    }
  }

  public Monitor getMonitor() {
    if (tokenMonitor == null) {
      tokenMonitor = new TokenMonitor((TokenWallet) getWallet());
    }
    return tokenMonitor;
  }

  public void setMonitor(Monitor monitor) {
    if (monitor.getClass().isInstance(TokenMonitor.class)) {
      tokenMonitor = (TokenMonitor) monitor;
    }
  }
}
