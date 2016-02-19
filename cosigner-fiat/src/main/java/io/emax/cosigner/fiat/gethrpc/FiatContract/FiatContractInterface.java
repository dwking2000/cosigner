package io.emax.cosigner.fiat.gethrpc.FiatContract;

public interface FiatContractInterface {
  String getCreateTokens();

  String getDestroyTokens();

  String getGetBalance();

  String getGetOwners();

  String getGetTotalBalance();

  String getIsOwner();

  String getTransfer();

  String getInitData();

  String getContractPayload();

  FiatContractParametersInterface getContractParameters();
}
