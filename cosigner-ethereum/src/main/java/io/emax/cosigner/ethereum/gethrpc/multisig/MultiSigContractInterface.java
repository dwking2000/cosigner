package io.emax.cosigner.ethereum.gethrpc.multisig;

public interface MultiSigContractInterface {
  String getInitData();

  String getContractPayload();

  String getExecuteFunctionAddress();

  String getKillFunctionAddress();

  String getIsOwnerFunctionAddress();

  String getGetOwnersFunctionAddress();

  MultiSigContractParametersInterface getContractParameters();
}
