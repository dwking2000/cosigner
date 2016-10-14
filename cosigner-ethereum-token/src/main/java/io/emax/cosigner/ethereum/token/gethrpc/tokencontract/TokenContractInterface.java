package io.emax.cosigner.ethereum.token.gethrpc.tokencontract;

import java.util.List;
import java.util.Map;

public interface TokenContractInterface {
  String ValueParam = "Value";
  String ArrayParam = "Array";

  Map<String, List<String>> getAdminParamTypes();

  String getCreateTokens();

  String getDestroyTokens();

  String getReconcile();

  String getScheduleVesting();

  String getCalculateVesting();

  String getGetBalance();

  String getGetTotalBalance();

  String getTransfer();

  String getAdminInitData();

  String getAdminRuntime();

  String getTokenInitData();

  String getTokenRuntime();

  String getStorageInitData();

  String getAlternateStorageInitData();

  String getStorageRuntime();

  String getAlternateStorageRunTime();

  String getSetTokenContract();

  TokenContractParametersInterface getContractParameters();
}
