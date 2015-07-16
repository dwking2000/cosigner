package io.emax.heimdal.blockchains.ethereum;


public interface Contract {
  String getAddress();

  String getContract();

  String getCode();
}
