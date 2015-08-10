package io.emax.heimdal.ethereum;

import java.math.BigDecimal;

public class Wallet implements io.emax.heimdal.api.currency.Wallet {

  @Override
  public String createAddress(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Iterable<String> getAddresses(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getMultiSigAddress(Iterable<String> addresses, String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getBalance(String address) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String createTransaction(Iterable<String> fromAddress, String toAddress,
      BigDecimal amount) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String signTransaction(String transaction, String address) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String signTransaction(String transaction, String address, String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String sendTransaction(String transaction) {
    // TODO Auto-generated method stub
    return null;
  }

}
