package io.emax.cosigner.ethereum.token.CurrencyConfigurations;

import io.emax.cosigner.api.core.CurrencyPackageInterface;
import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.Monitor;
import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.ethereum.token.TokenConfiguration;
import io.emax.cosigner.ethereum.token.TokenMonitor;
import io.emax.cosigner.ethereum.token.TokenWallet;

public class EthCurrencyPackage implements CurrencyPackageInterface {
  private TokenConfiguration tokenConfiguration = new TokenConfiguration("ETH");
  private TokenWallet tokenWallet = null;
  private TokenMonitor tokenMonitor = null;

  @Override
  public CurrencyConfiguration getConfiguration() {
    return tokenConfiguration;
  }

  @Override
  public void setConfiguration(CurrencyConfiguration configuration) {
    if(configuration.getClass().isInstance(TokenConfiguration.class)) {
      tokenConfiguration = (TokenConfiguration) configuration;
    }
  }

  @Override
  public Wallet getWallet() {
    if(tokenWallet == null) {
      tokenWallet = new TokenWallet(tokenConfiguration);
    }
    return tokenWallet;
  }

  @Override
  public void setWallet(Wallet wallet) {
    if(wallet.getClass().isInstance(TokenWallet.class)) {
      tokenWallet = (TokenWallet) wallet;
    }
  }

  @Override
  public Monitor getMonitor() {
    if(tokenMonitor == null) {
      tokenMonitor = new TokenMonitor((TokenWallet) getWallet());
    }
    return tokenMonitor;
  }

  @Override
  public void setMonitor(Monitor monitor) {
    if(monitor.getClass().isInstance(TokenMonitor.class)) {
      tokenMonitor = (TokenMonitor) monitor;
    }
  }
}
