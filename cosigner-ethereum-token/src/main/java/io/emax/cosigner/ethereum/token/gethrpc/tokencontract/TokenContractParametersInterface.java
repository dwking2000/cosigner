package io.emax.cosigner.ethereum.token.gethrpc.tokencontract;

import io.emax.cosigner.ethereum.core.gethrpc.EthereumRpc;
import io.emax.cosigner.ethereum.token.TokenConfiguration;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public interface TokenContractParametersInterface {
  String FUNCTION = "function";
  String NONCE = "nonce";
  String SENDER = "sender";
  String RECIPIENTS = "recipients";
  String AMOUNT = "amount";
  String PARAM = "param";
  String SIGV = "sigV";
  String SIGR = "sigR";
  String SIGS = "sigS";

  Long getNonce(EthereumRpc ethereumRpc, String contractAddress);

  String calculateAdminHash(EthereumRpc ethereumRpc, String contractAddress);

  String calculateAdminHash(EthereumRpc ethereumRpc, String contractAddress, Long nonce);

  String calculateTxHash(EthereumRpc ethereumRpc, String contractAddress,
      List<String> recipients, List<String> amounts);

  String calculateTxHash(Long nonce, List<String> recipients, List<String> amounts);

  String createAdminContract(String adminAddress, List<String> ownerAddresses,
      int numSignaturesRequired);

  String createAdminContract(String adminAddress, List<String> ownerAddresses,
      int numSignaturesRequired, long securityValue);

  String createTokenContract(String parentAddress);

  String createTokenContract(String parentAddress, String name, String symbol, int decimals);

  String createStorageContract(TokenConfiguration config, String tokenContract, String adminAddress,
      List<String> ownerAddresses, int numSignaturesRequired);

  String createStorageContract(TokenConfiguration config, String tokenContract, String adminAddress,
      List<String> ownerAddresses, int numSignaturesRequired, long securityValue, String name,
      String symbol, int decimals);

  String setTokenChild(long nonce, String childAddress, List<String> sigV, List<String> sigR,
      List<String> sigS);

  String createTokens(long nonce, String recipient, long numTokens, List<String> sigV,
      List<String> sigR, List<String> sigS);

  String destroyTokens(long nonce, String sender, long numTokens, List<String> sigV,
      List<String> sigR, List<String> sigS);

  String reconcile(long nonce, Map<String, BigInteger> addressChanges, List<String> sigV,
      List<String> sigR, List<String> sigS);

  String getBalance(String address);

  String getTotalBalance();

  String deposit(TokenConfiguration config, String recipient, BigInteger amount);

  String tokenTransfer(String recipient, BigInteger amount);

  String transfer(long nonce, String sender, List<String> recipients, List<BigInteger> amount,
      List<String> sigV, List<String> sigR, List<String> sigS);

  Map<String, List<String>> parseTransfer(String bytecode);

  Map<String, List<String>> parseAdminFunction(String bytecode);

  String rebuildAdminFunction(Map<String, List<String>> params);
}
