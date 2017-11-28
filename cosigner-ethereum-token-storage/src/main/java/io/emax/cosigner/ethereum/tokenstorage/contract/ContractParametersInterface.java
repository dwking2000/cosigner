package io.emax.cosigner.ethereum.tokenstorage.contract;

import io.emax.cosigner.ethereum.core.gethrpc.EthereumRpc;
import io.emax.cosigner.ethereum.tokenstorage.Configuration;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public interface ContractParametersInterface {
  String FUNCTION = "function";
  String NONCE = "nonce";
  String SENDER = "sender";
  String RECIPIENTS = "recipients";
  String AMOUNT = "amount";
  String PARAM = "param";
  String SIGV = "sigV";
  String SIGR = "sigR";
  String SIGS = "sigS";

  Long getNonce(EthereumRpc ethereumRpc, String contractAddress, String senderAddress)
      throws Exception;

  String calculateAdminHash(EthereumRpc ethereumRpc, String contractAddress) throws Exception;

  String calculateAdminHash(EthereumRpc ethereumRpc, String contractAddress, Long nonce)
      throws Exception;

  String calculateTxHash(EthereumRpc ethereumRpc, String contractAddress, String sender,
      List<String> recipients, List<String> amounts) throws Exception;

  String calculateTxHash(String contractAddress, Long nonce, String sender, List<String> recipients, List<String> amounts)
      throws Exception;

  @Deprecated
  String createAdminContract(String adminAddress, List<String> ownerAddresses,
      int numSignaturesRequired);

  @Deprecated
  String createAdminContract(String adminAddress, List<String> ownerAddresses,
      int numSignaturesRequired, long securityValue);

  @Deprecated
  String createTokenContract(String parentAddress);

  @Deprecated
  String createTokenContract(String parentAddress, String name, String symbol, int decimals);

  String createStorageContract(Configuration config, String tokenContract, String adminAddress,
      List<String> ownerAddresses, int numSignaturesRequired);

  String createStorageContract(Configuration config, String tokenContract, String adminAddress,
      List<String> ownerAddresses, int numSignaturesRequired, long securityValue, String name,
      String symbol, int decimals);

  String setTokenChild(long nonce, String childAddress, List<String> sigV, List<String> sigR,
      List<String> sigS);

  @Deprecated
  String createTokens(long nonce, String recipient, long numTokens, List<String> sigV,
      List<String> sigR, List<String> sigS);

  @Deprecated
  String destroyTokens(long nonce, String sender, long numTokens, List<String> sigV,
      List<String> sigR, List<String> sigS);

  String reconcile(long nonce, Map<String, BigInteger> addressChanges, List<String> sigV,
      List<String> sigR, List<String> sigS);

  String getBalance(String address);

  String getTotalBalance();

  String deposit(Configuration config, String recipient, BigInteger amount);

  @Deprecated
  String tokenTransfer(String recipient, BigInteger amount);

  String offlineTransfer(long nonce, String sender, List<String> recipients,
      List<BigInteger> amount, List<String> sigV, List<String> sigR, List<String> sigS);

  @Deprecated
  String approve(String grantee, BigInteger amount);

  @Deprecated
  String allowance(String owner, String grantee);

  Map<String, List<String>> parseTransfer(String bytecode);

  Map<String, List<String>> parseAdminFunction(String bytecode);

  String rebuildAdminFunction(Map<String, List<String>> params);
}
