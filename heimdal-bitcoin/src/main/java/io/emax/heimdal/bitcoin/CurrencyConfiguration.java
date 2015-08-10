package io.emax.heimdal.bitcoin;

import java.util.LinkedList;

// TODO Make this configurable via config file. Being lazy for now.
public class CurrencyConfiguration implements io.emax.heimdal.api.currency.CurrencyConfiguration {

  @Override
  public String getCurrencySymbol() {
    return "BTC";
  }

  public int getMinConfirmations() {
    return 6;
  }

  public int getMaxConfirmations() {
    return 9999999;
  }

  public String getDaemonConnectionString() {
    return "http://127.0.0.1:18332";
  }

  public int getMinSignatures() {
    return 1;
  }

  public Iterable<String> getBaseMultiSigAccounts() {
    return new LinkedList<String>();
  }

  public String getServerPrivateKey() {
    // TODO Load this somehow, don't just leave it in the code.
    return "b0837faed56bc7c48dc29d564b1c030f03eee53b0317c53d784c8f40654821c6";
  }

  public int getMaxDeterministicAddresses() {
    return 100;
  }

  @Override
  public void getSigningType() {
    // TODO Auto-generated method stub

  }

}
