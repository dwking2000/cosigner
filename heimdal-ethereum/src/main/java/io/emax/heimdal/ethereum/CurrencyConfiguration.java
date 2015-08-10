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

}
