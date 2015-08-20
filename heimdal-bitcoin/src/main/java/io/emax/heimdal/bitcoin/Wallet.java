package io.emax.heimdal.bitcoin;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.emax.heimdal.bitcoin.bitcoind.BitcoindRpc;
import io.emax.heimdal.bitcoin.bitcoind.DecodedTransaction;
import io.emax.heimdal.bitcoin.bitcoind.DecodedTransaction.DecodedInput;
import io.emax.heimdal.bitcoin.bitcoind.MultiSig;
import io.emax.heimdal.bitcoin.bitcoind.Outpoint;
import io.emax.heimdal.bitcoin.bitcoind.OutpointDetails;
import io.emax.heimdal.bitcoin.bitcoind.Output;
import io.emax.heimdal.bitcoin.bitcoind.SigHash;
import io.emax.heimdal.bitcoin.bitcoind.SignedTransaction;
import io.emax.heimdal.bitcoin.common.DeterministicTools;

public class Wallet implements io.emax.heimdal.api.currency.Wallet {

  private CurrencyConfiguration config = new CurrencyConfiguration();
  private BitcoindResource bitcoind = BitcoindResource.getResource();
  private BitcoindRpc bitcoindRpc = bitcoind.getBitcoindRpc();

  @Override
  public String createAddress(String name) {
    int rounds = 1;
    String privateKey =
        DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);
    String newAddress = DeterministicTools.getPublicAddress(privateKey);
    // Hash the user's key so it's not stored in the wallet
    String internalName = "Single-" + DeterministicTools.encodeUserKey(name);

    String[] existingAddresses = bitcoindRpc.getaddressesbyaccount(internalName);
    boolean oldAddress = true;

    while (oldAddress && rounds <= config.getMaxDeterministicAddresses()) {
      oldAddress = false;
      for (int i = 0; i < existingAddresses.length; i++) {
        if (existingAddresses[i].equalsIgnoreCase(newAddress)) {
          oldAddress = true;
          rounds++;
          privateKey = DeterministicTools.getDeterministicPrivateKey(name,
              config.getServerPrivateKey(), rounds);
          newAddress = DeterministicTools.getPublicAddress(privateKey);
          break;
        }
      }
    }
    bitcoindRpc.importaddress(newAddress, internalName, false);

    return newAddress;
  }

  @Override
  public Iterable<String> getAddresses(String name) {
    // Hash the user's key so it's not stored in the wallet
    String internalName = DeterministicTools.encodeUserKey(name);

    String[] addresses = bitcoindRpc.getaddressesbyaccount(internalName);
    return Arrays.asList(addresses);
  }

  @Override
  public String getMultiSigAddress(Iterable<String> addresses, String name) {
    // Hash the user's key so it's not stored in the wallet
    String internalName = DeterministicTools.encodeUserKey(name);
    String newAddress = generateMultiSigAddress(addresses, name);
    bitcoindRpc.importaddress(newAddress, internalName, false);

    return newAddress;
  }

  public String generateMultiSigAddress(Iterable<String> addresses, String name) {
    LinkedList<String> multisigAddresses = new LinkedList<>();
    addresses.forEach((address) -> {
      // Check if any of the addresses belong to the user
      int rounds = 1;
      String userPrivateKey =
          DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);
      String userAddress = DeterministicTools.getPublicAddress(userPrivateKey);

      while (!address.equalsIgnoreCase(userAddress)
          && rounds <= config.getMaxDeterministicAddresses()) {
        rounds++;
        userPrivateKey = DeterministicTools.getDeterministicPrivateKey(name,
            config.getServerPrivateKey(), rounds);
        userAddress = DeterministicTools.getPublicAddress(userPrivateKey);
      }

      if (address.equalsIgnoreCase(userAddress)) {
        multisigAddresses.add(DeterministicTools.getPublicKey(userPrivateKey));
      } else {
        multisigAddresses.add(address);
      }
    });
    for (String account : config.getMultiSigAccounts()) {
      multisigAddresses.add(account);
    }

    String[] addressArray = new String[multisigAddresses.size()];
    MultiSig newAddress = bitcoindRpc.createmultisig(config.getMinSignatures(),
        multisigAddresses.toArray(addressArray));

    return newAddress.getAddress();
  }

  @Override
  public String getBalance(String address) {
    BigDecimal balance = BigDecimal.ZERO;
    Output[] outputs = bitcoindRpc.listunspent(config.getMinConfirmations(),
        config.getMaxConfirmations(), new String[] {address});
    for (Output output : outputs) {
      balance = balance.add(output.getAmount());
    }
    return balance.toPlainString();
  }

  @Override
  public String createTransaction(Iterable<String> fromAddress, String toAddress,
      BigDecimal amount) {
    List<String> fromAddresses = new LinkedList<>();
    fromAddress.forEach(fromAddresses::add);
    String[] addresses = new String[fromAddresses.size()];
    Outpoint[] outputs = bitcoindRpc.listunspent(config.getMinConfirmations(),
        config.getMaxConfirmations(), fromAddresses.toArray(addresses));

    List<Outpoint> usedOutputs = new LinkedList<>();
    Map<String, BigDecimal> txnOutput = new HashMap<>();
    BigDecimal total = BigDecimal.ZERO;
    for (Outpoint output : outputs) {
      total = total.add(output.getAmount());
      usedOutputs.add(output);

      if (total.compareTo(amount) >= 0) {
        txnOutput.put(toAddress, amount);
        if (total.compareTo(amount) > 0) {
          // TODO Consider generating change address, don't just put
          // it back in the first one
          // TODO don't hardcode fees
          txnOutput.put(fromAddress.iterator().next(),
              total.subtract(amount).subtract(new BigDecimal("0.002")));
        }
        break;
      }
    }

    // We don't have enough to complete the transaction
    if (txnOutput.size() == 0) {
      return null;
    }

    Outpoint[] usedOutputArray = new Outpoint[usedOutputs.size()];
    return bitcoindRpc.createrawtransaction(usedOutputs.toArray(usedOutputArray), txnOutput);
  }

  @Override
  public String signTransaction(String transaction, String address) {
    return signTransaction(transaction, address, null);
  }

  @Override
  public String signTransaction(String transaction, String address, String name) {
    int rounds = 1;
    String privateKey = "";
    String userAddress = "";
    SignedTransaction signedTransaction = null;

    if (name != null) {
      privateKey =
          DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);
      userAddress = DeterministicTools.getPublicAddress(privateKey);
      while (!generateMultiSigAddress(Arrays.asList(new String[] {userAddress}), name)
          .equalsIgnoreCase(address) && !userAddress.equalsIgnoreCase(address)
          && rounds < config.getMaxDeterministicAddresses()) {
        rounds++;
        privateKey = DeterministicTools.getDeterministicPrivateKey(name,
            config.getServerPrivateKey(), rounds);
        userAddress = DeterministicTools.getPublicAddress(privateKey);
      }

      // If we hit max addresses/user bail out
      if (!generateMultiSigAddress(Arrays.asList(new String[] {userAddress}), name)
          .equalsIgnoreCase(address) && !userAddress.equalsIgnoreCase(address)) {
        return transaction;
      }

      // We have the private key, now get all the unspent inputs so we have the redeemScripts.
      DecodedTransaction myTx = bitcoindRpc.decoderawtransaction(transaction);
      List<DecodedInput> inputs = myTx.getInputs();
      Outpoint[] outputs = bitcoindRpc.listunspent(config.getMinConfirmations(),
          config.getMaxConfirmations(), new String[] {});
      List<OutpointDetails> myOutpoints = new LinkedList<>();
      inputs.forEach((input) -> {
        for (Outpoint output : outputs) {
          if (output.getTransactionId().equalsIgnoreCase(input.getTransactionId())
              && output.getOutputIndex() == input.getOutputIndex()) {
            OutpointDetails outpoint = new OutpointDetails();
            outpoint.setTransactionId(output.getTransactionId());
            outpoint.setOutputIndex(output.getOutputIndex());
            outpoint.setScriptPubKey(output.getScriptPubKey());
            outpoint.setRedeemScript(output.getRedeemScript());
            myOutpoints.add(outpoint);
          }
        }
      });

      OutpointDetails[] outpointArray = new OutpointDetails[myOutpoints.size()];
      outpointArray = myOutpoints.toArray(outpointArray);

      signedTransaction = bitcoindRpc.signrawtransaction(transaction, outpointArray,
          new String[] {privateKey}, SigHash.ALL);
    } else {
      // If we're not restricting the keystore, bitcoind knows about the redeem script
      signedTransaction =
          bitcoindRpc.signrawtransaction(transaction, new OutpointDetails[] {}, null, SigHash.ALL);
    }

    return signedTransaction.getTransaction();
  }

  @Override
  public String sendTransaction(String transaction) {
    return bitcoindRpc.sendrawtransaction(transaction, false);
  }
}
