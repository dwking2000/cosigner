package io.emax.heimdal.bitcoin;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.emax.heimdal.bitcoin.bitcoindrpc.BitcoindRpc;
import io.emax.heimdal.bitcoin.bitcoindrpc.MultiSig;
import io.emax.heimdal.bitcoin.bitcoindrpc.Outpoint;
import io.emax.heimdal.bitcoin.bitcoindrpc.OutpointDetails;
import io.emax.heimdal.bitcoin.bitcoindrpc.Output;
import io.emax.heimdal.bitcoin.bitcoindrpc.RawInput;
import io.emax.heimdal.bitcoin.bitcoindrpc.RawOutput;
import io.emax.heimdal.bitcoin.bitcoindrpc.RawTransaction;
import io.emax.heimdal.bitcoin.bitcoindrpc.SigHash;
import io.emax.heimdal.bitcoin.bitcoindrpc.SignedTransaction;
import io.emax.heimdal.bitcoin.common.ByteUtilities;
import io.emax.heimdal.bitcoin.common.DeterministicTools;
import io.emax.heimdal.bitcoin.common.Secp256k1;

public class Wallet implements io.emax.heimdal.api.currency.Wallet {

  private CurrencyConfiguration config = new CurrencyConfiguration();
  private BitcoindRpc bitcoindRpc = BitcoindResource.getResource().getBitcoindRpc();

  private static HashMap<String, String> multiSigRedeemScripts = new HashMap<>();

  public Wallet(BitcoindRpc rpc) {
    this.bitcoindRpc = rpc;
  }

  public Wallet() {

  }

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
      if (!account.isEmpty()) {
        multisigAddresses.add(account);
      }
    }

    String[] addressArray = new String[multisigAddresses.size()];
    MultiSig newAddress = bitcoindRpc.createmultisig(config.getMinSignatures(),
        multisigAddresses.toArray(addressArray));

    multiSigRedeemScripts.put(newAddress.getAddress(), newAddress.getRedeemScript());

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
  public String createTransaction(Iterable<String> fromAddress, Iterable<Recipient> toAddress) {
    List<String> fromAddresses = new LinkedList<>();
    fromAddress.forEach(fromAddresses::add);
    String[] addresses = new String[fromAddresses.size()];
    Outpoint[] outputs = bitcoindRpc.listunspent(config.getMinConfirmations(),
        config.getMaxConfirmations(), fromAddresses.toArray(addresses));

    List<Outpoint> usedOutputs = new LinkedList<>();
    Map<String, BigDecimal> txnOutput = new HashMap<>();
    BigDecimal total = BigDecimal.ZERO;
    BigDecimal subTotal = BigDecimal.ZERO;
    Iterator<Recipient> recipients = toAddress.iterator();
    Recipient recipient = recipients.next();
    boolean filledAllOutputs = false;
    for (Outpoint output : outputs) {
      total = total.add(output.getAmount());
      subTotal = subTotal.add(output.getAmount());
      usedOutputs.add(output);

      if (subTotal.compareTo(recipient.getAmount()) > 0) {
        txnOutput.put(recipient.getRecipientAddress(), recipient.getAmount());
        subTotal = subTotal.subtract(recipient.getAmount());
        if (recipients.hasNext()) {
          recipient = recipients.next();
        } else {
          // TODO don't hardcode fees -- 0.0001 BTC * KB suggested by spec
          txnOutput.put(fromAddress.iterator().next(), subTotal.subtract(new BigDecimal("0.002")));
          filledAllOutputs = true;
        }
        break;
      }
    }

    // We don't have enough to complete the transaction
    if (!filledAllOutputs) {
      return null;
    }

    RawTransaction rawTx = new RawTransaction();
    rawTx.setVersion(1);
    rawTx.setInputCount(usedOutputs.size());
    usedOutputs.forEach((input) -> {
      RawInput rawInput = new RawInput();
      rawInput.setTxHash(input.getTransactionId());
      rawInput.setTxIndex((int) input.getOutputIndex());
      rawInput.setSequence(-1);
      rawTx.getInputs().add(rawInput);
    });
    rawTx.setOutputCount(txnOutput.size());
    txnOutput.forEach((address, amount) -> {
      RawOutput rawOutput = new RawOutput();
      rawOutput.setAmount(amount.multiply(BigDecimal.valueOf(100000000)).longValue());
      String decodedAddress = DeterministicTools.decodeAddress(address);
      byte[] addressBytes = ByteUtilities.toByteArray(decodedAddress);
      String scriptData = "";
      if (!DeterministicTools.isMultiSigAddress(address)) {
        // Regular address
        scriptData = "76a914";
        scriptData += ByteUtilities.toHexString(addressBytes);
        scriptData += "88ac";
      } else {
        // Multi-sig address
        scriptData = "a914";
        scriptData += ByteUtilities.toHexString(addressBytes);
        scriptData += "87";
      }
      rawOutput.setScript(scriptData);
      rawTx.getOutputs().add(rawOutput);
    });
    rawTx.setLockTime(0);

    return rawTx.encode();

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
      Outpoint[] outputs = bitcoindRpc.listunspent(config.getMinConfirmations(),
          config.getMaxConfirmations(), new String[] {});

      RawTransaction rawTx = RawTransaction.parse(transaction);
      final byte[] addressData = DeterministicTools.getPublicKeyBytes(privateKey);
      final byte[] privateKeyBytes =
          ByteUtilities.toByteArray(DeterministicTools.decodeAddress(privateKey));
      rawTx.getInputs().forEach((input) -> {
        for (Outpoint output : outputs) {
          if (output.getTransactionId().equalsIgnoreCase(input.getTxHash())
              && output.getOutputIndex() == input.getTxIndex()) {
            OutpointDetails outpoint = new OutpointDetails();
            outpoint.setTransactionId(output.getTransactionId());
            outpoint.setOutputIndex(output.getOutputIndex());
            outpoint.setScriptPubKey(output.getScriptPubKey());
            outpoint.setRedeemScript(multiSigRedeemScripts.get(output.getAddress()));

            if (output.getAddress().equalsIgnoreCase(address)) {
              RawTransaction signingTx = RawTransaction.stripInputScripts(rawTx);
              byte[] sigData = new byte[] {};

              for (RawInput sigInput : signingTx.getInputs()) {
                if (sigInput.getTxHash().equalsIgnoreCase(outpoint.getTransactionId())
                    && sigInput.getTxIndex() == outpoint.getOutputIndex()) {
                  // This is the input we're processing, fill it and sign it
                  sigInput.setScript(outpoint.getScriptPubKey());
                  byte[] hashTypeBytes =
                      ByteUtilities.stripLeadingNullBytes(BigInteger.valueOf(1).toByteArray());
                  hashTypeBytes = ByteUtilities.leftPad(hashTypeBytes, 4, (byte) 0x00);
                  hashTypeBytes = ByteUtilities.flipEndian(hashTypeBytes);
                  String sigString = signingTx.encode() + ByteUtilities.toHexString(hashTypeBytes);

                  try {
                    sigData = ByteUtilities.toByteArray(sigString);
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    sigData = md.digest(md.digest(sigData));
                  } catch (Exception e) {
                    e.printStackTrace();
                  }

                  sigData = Secp256k1.signTransaction(sigData, privateKeyBytes);
                  break;
                }
              }

              // Determine how we need to format the sig data
              if (DeterministicTools.isMultiSigAddress(address)) {
                // TODO - Implement multi-sig signing
              } else {
                for (RawInput signedInput : rawTx.getInputs()) {
                  if (signedInput.getTxHash().equalsIgnoreCase(outpoint.getTransactionId())
                      && signedInput.getTxIndex() == outpoint.getOutputIndex()) {

                    // Sig then pubkey
                    String scriptData = "";
                    byte[] dataSize = new byte[] {};

                    dataSize = BigInteger.valueOf(sigData.length + 1).toByteArray();
                    dataSize = ByteUtilities.stripLeadingNullBytes(dataSize);
                    scriptData += ByteUtilities.toHexString(dataSize);
                    scriptData += ByteUtilities.toHexString(sigData);
                    scriptData += "01"; // SIGHASH.ALL

                    dataSize = BigInteger.valueOf(addressData.length).toByteArray();
                    dataSize = ByteUtilities.stripLeadingNullBytes(dataSize);
                    scriptData += ByteUtilities.toHexString(dataSize);
                    scriptData += ByteUtilities.toHexString(addressData);

                    signedInput.setScript(scriptData);
                    break;
                  }
                }
              }
            }
          }
        }
      });
      signedTransaction = new SignedTransaction();
      signedTransaction.setTransaction(rawTx.encode());
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
