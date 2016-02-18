package io.emax.cosigner.fiat.gethrpc.multisig;

public interface MultiSigContractInterface {
  String getInitData();

  String getContractPayload();

  String getExecuteFunctionAddress();

  // TODO GenerateTokenAddress
  // TODO DestroyTokenAddress

  // TODO Remove KillFunction
  String getKillFunctionAddress();

  // TODO Owners Vs Administrators
  String getIsOwnerFunctionAddress();

  String getGetOwnersFunctionAddress();

  MultiSigContractParametersInterface getContractParameters();
}
