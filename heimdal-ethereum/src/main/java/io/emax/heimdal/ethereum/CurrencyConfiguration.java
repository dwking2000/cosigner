package io.emax.heimdal.ethereum;

public class CurrencyConfiguration implements io.emax.heimdal.api.currency.CurrencyConfiguration {
  private String daemonConnectionString = "http://localhost:8101";

  @Override
  public String getCurrencySymbol() {
    return "ETH";
  }

  @Override
  public void getSigningType() {
    // TODO Auto-generated method stub

  }

  public String getDaemonConnectionString() {
    return daemonConnectionString;
  }

  public String getServerPrivateKey() {
    // TODO Load this somehow, don't just leave it in the code.
    return "b0837faed56bc7c48dc29d564b1c030f03eee53b0317c53d784c8f40654821c6";
  }

  public int getMinConfirmations() {
    return 10; // TODO don't use 10
  }

  public long getGasPrice() {
    return 60000000000L;
  }

  public long getSimpleTxGas() {
    return 90000L;
  }

  public long getWeiMultiplier() {
    return 1000000000000000000L;
  }
}
