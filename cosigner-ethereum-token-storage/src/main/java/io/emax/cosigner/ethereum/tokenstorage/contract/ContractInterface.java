package io.emax.cosigner.ethereum.tokenstorage.contract;

import java.util.List;
import java.util.Map;

public interface ContractInterface {
  String ValueParam = "Value";
  String ArrayParam = "Array";

  Map<String, List<String>> getAdminParamTypes();

  /**
   * @deprecated We will no longer be supporting ERC-20 tokens in this library.
   */
  @Deprecated
  String getCreateTokens();

  /**
   * @deprecated We will no longer be supporting ERC-20 tokens in this library.
   */
  @Deprecated
  String getDestroyTokens();

  String getReconcile();

  String getDeposit();

  String getGetBalance();

  String getGetTotalBalance();

  /**
   * @deprecated We will no longer be supporting ERC-20 tokens in this library.
   */
  @Deprecated
  String getTransfer();

  String getTokenTransfer();

  /**
   * @deprecated We will no longer be managing ERC-20 token contracts in this library.
   */
  @Deprecated
  String getAdminInitData();

  /**
   * @deprecated We will no longer be managing ERC-20 token contracts in this library.
   */
  @Deprecated
  String getAdminRuntime();

  /**
   * @deprecated We will no longer be managing ERC-20 token contracts in this library.
   */
  @Deprecated
  String getTokenInitData();

  /**
   * @deprecated We will no longer be managing ERC-20 token contracts in this library.
   */
  @Deprecated
  String getTokenRuntime();

  String getStorageInitData();

  String getAlternateStorageInitData();

  String getStorageRuntime();

  String getAlternateStorageRunTime();

  String getAlternateDeposit();

  String getSetTokenContract();

  ContractParametersInterface getContractParameters();

  Boolean hasAdminAndTokenContracts();

  String getGetTxHash();

  String getSecurityValue();
}
