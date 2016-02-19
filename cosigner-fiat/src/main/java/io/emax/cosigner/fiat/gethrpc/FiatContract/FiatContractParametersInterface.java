package io.emax.cosigner.fiat.gethrpc.FiatContract;

import java.util.List;
import java.util.Map;

public interface FiatContractParametersInterface {
  String createContract(String adminAddress, List<String> ownerAddresses,
      int numSignaturesRequired);

  String createTokens(long numTokens);

  String destroyTokens(long numTokens);

  String getOwners();

  String getBalance(String address);

  String getTotalBalance();

  String isOwner(String address);

  String transfer(long nonce, String sender, List<String> recipients, List<Long> amount,
      List<String> sigV, List<String> sigR, List<String> sigS);

  Map<String, List<String>> parseTransfer(String bytecode);
}
