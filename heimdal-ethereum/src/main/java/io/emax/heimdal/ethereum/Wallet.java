package io.emax.heimdal.ethereum;

import java.math.BigDecimal;
import java.math.BigInteger;

import io.emax.heimdal.ethereum.gethrpc.EthereumRpc;

public class Wallet implements io.emax.heimdal.api.currency.Wallet {
  private EthereumRpc ethereumRpc = EthereumResource.getResource().getBitcoindRpc();
  
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
    return new BigInteger(ethereumRpc.eth_getBalance(address, "latest").substring(2), 16).toString(10);          
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
