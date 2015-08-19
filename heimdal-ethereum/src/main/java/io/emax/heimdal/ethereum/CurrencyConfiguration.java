package io.emax.heimdal.ethereum;

import io.emax.heimdal.api.currency.SigningType;

public class CurrencyConfiguration implements io.emax.heimdal.api.currency.CurrencyConfiguration {
  private String daemonConnectionString = "http://localhost:8101";

  @Override
  public String getCurrencySymbol() {
    return "ETH";
  }

  @Override
  public SigningType getSigningType() {
    return SigningType.SENDEACH;
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
    return 100000000000L;
  }

  public long getSimpleTxGas() {
    return 90000L;
  }
  
  public long getMsigTxGas() {
    return 3000000L;
  }
 
  public int getMinSignatures(){
    return 2;
  }
  
  public String[] getMsigAddresses(){
    return new String[] {"4839540a0ae3242fadf288622f7de1a9278a5858"};
  }

  public long getWeiMultiplier() {
    return 1000000000000000000L;
  }
  
  public String getContractAccount(){
    return "4839540a0ae3242fadf288622f7de1a9278a5858";
  }
}
