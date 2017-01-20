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

  String createAdminContract(String adminAddress, List<String> ownerAddresses,
      int numSignaturesRequired);

  String createTokenContract(String parentAddress);

  String createStorageContract(TokenConfiguration config, String tokenContract, String adminAddress,
      List<String> ownerAddresses, int numSignaturesRequired);

  String setTokenChild(long nonce, String childAddress, List<String> sigV, List<String> sigR,
      List<String> sigS);

  String createTokens(long nonce, String recipient, long numTokens, List<String> sigV,
      List<String> sigR, List<String> sigS);

  String destroyTokens(long nonce, String sender, long numTokens, List<String> sigV,
      List<String> sigR, List<String> sigS);

  String reconcile(long nonce, Map<String, BigInteger> addressChanges, List<String> sigV,
      List<String> sigR, List<String> sigS);

  String scheduleVesting(long nonce, String address, BigInteger amount, BigInteger timeFrame,
      Boolean prorated, List<String> sigV, List<String> sigR, List<String> sigS);

  String calculateVesting(long nonce, List<String> sigV, List<String> sigR, List<String> sigS);

  String getBalance(String address);

  String getTotalBalance();

  String deposit(String recipient, BigInteger amount);

  String tokenTransfer(String recipient, BigInteger amount);

  String transfer(long nonce, String sender, List<String> recipients, List<BigInteger> amount,
      List<String> sigV, List<String> sigR, List<String> sigS);

  Map<String, List<String>> parseTransfer(String bytecode);

  Map<String, List<String>> parseAdminFunction(String bytecode);

  String rebuildAdminFunction(Map<String, List<String>> params);
}
