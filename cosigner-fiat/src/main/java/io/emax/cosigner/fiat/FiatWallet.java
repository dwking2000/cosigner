package io.emax.cosigner.fiat;

import io.emax.cosigner.api.core.ServerStatus;
import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.crypto.Secp256k1;
import io.emax.cosigner.ethereum.EthereumResource;
import io.emax.cosigner.ethereum.common.EthereumTools;
import io.emax.cosigner.ethereum.common.RlpItem;
import io.emax.cosigner.ethereum.common.RlpList;
import io.emax.cosigner.ethereum.gethrpc.CallData;
import io.emax.cosigner.ethereum.gethrpc.DefaultBlock;
import io.emax.cosigner.ethereum.gethrpc.EthereumRpc;
import io.emax.cosigner.ethereum.gethrpc.RawTransaction;
import io.emax.cosigner.fiat.gethrpc.FiatContract.FiatContract;
import io.emax.cosigner.fiat.gethrpc.FiatContract.FiatContractInterface;
import io.emax.cosigner.fiat.gethrpc.FiatContract.FiatContractParametersInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FiatWallet implements Wallet {
  private static final Logger LOGGER = LoggerFactory.getLogger(FiatWallet.class);

  // RPC and configuration
  private static final EthereumRpc ethereumRpc = EthereumResource.getResource().getGethRpc();
  private static FiatConfiguration config;

  private String contractAddress = "";
  private FiatContractInterface contractInterface = new FiatContract();
  private HashSet<String> knownAddresses = new HashSet<>();
  private HashMap<String, HashSet<String>> ownedAddresses = new HashMap<>();

  public FiatWallet(String currency) {
    config = new FiatConfiguration(currency);
    setupFiatContract();
  }

  private void setupFiatContract() {
    if (config.getContractAddress() != null && !config.getContractAddress().isEmpty()) {
      contractAddress = config.getContractAddress();
      contractInterface = getContractType(contractAddress);
    } else {
      try {
        String txCount = ethereumRpc.eth_getTransactionCount("0x" + config.getContractAccount(),
            DefaultBlock.LATEST.toString());
        int rounds = new BigInteger(1, ByteUtilities.toByteArray(txCount)).intValue();
        for (int i = 0; i < rounds; i++) {
          RlpList contractAddress = new RlpList();
          RlpItem contractCreator =
              new RlpItem(ByteUtilities.toByteArray(config.getContractAccount()));
          RlpItem nonce =
              new RlpItem(ByteUtilities.stripLeadingNullBytes(BigInteger.valueOf(i).toByteArray()));
          contractAddress.add(contractCreator);
          contractAddress.add(nonce);

          String contract =
              EthereumTools.hashKeccak(ByteUtilities.toHexString(contractAddress.encode()))
                  .substring(96 / 4, 256 / 4);
          FiatContractInterface contractClass = getContractType(contract);
          if (contractClass != null) {
            this.contractInterface = contractClass;
            this.contractAddress = contract;
            break;
          }
        }

      } catch (Exception e) {
        LOGGER.debug(null, e);
      }
    }

    if ((contractAddress == null || contractAddress.isEmpty()) && config.generateNewContract()) {
      LOGGER.debug("Generating new contract...");
      // Create the TX data structure
      RawTransaction tx = new RawTransaction();
      tx.getGasPrice().setDecodedContents(ByteUtilities
          .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
      tx.getGasLimit().setDecodedContents(ByteUtilities
          .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));
      tx.getData().setDecodedContents(ByteUtilities.toByteArray(
          contractInterface.getContractParameters()
              .createContract(config.getAdminAccount(), Arrays.asList(config.getMultiSigAccounts()),
                  config.getMinSignatures())));

      String rawTx = ByteUtilities.toHexString(tx.encode());
      LOGGER.debug("Creating contract: " + rawTx);
      sendTransaction(rawTx);
    }
  }

  private FiatContractInterface getContractType(String contract) {
    try {
      String contractCode = ethereumRpc
          .eth_getCode("0x" + contract.toLowerCase(Locale.US), DefaultBlock.LATEST.toString());
      contractCode = contractCode.substring(2);
      LOGGER.debug("Contract code: " + contractCode);
      Class<?> contractType = FiatContract.class;
      while (FiatContractInterface.class.isAssignableFrom(contractType)) {
        FiatContractInterface contractParams = (FiatContractInterface) contractType.newInstance();
        if (contractParams.getContractPayload().equalsIgnoreCase(contractCode)) {
          return contractParams;
        }
        contractType = contractType.getSuperclass();
      }
    } catch (Exception e) {
      LOGGER.debug(null, e);
      return null;
    }
    return null;
  }

  @Override
  public String createAddress(String name) {
    return createAddress(name, 0);
  }

  @Override
  public String createAddress(String name, int skipNumber) {
    // Generate the next private key
    LOGGER.debug("Creating a new normal address...");
    String user = EthereumTools.encodeUserKey(name);
    if (!ownedAddresses.containsKey(user)) {
      getAddresses(name);
    }

    int rounds = 1 + skipNumber;
    String privateKey =
        EthereumTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);

    // Convert to an Ethereum address
    String publicAddress = EthereumTools.getPublicAddress(privateKey);

    while (knownAddresses.contains(publicAddress.toLowerCase(Locale.US))) {
      rounds++;
      privateKey =
          EthereumTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);
      publicAddress = EthereumTools.getPublicAddress(privateKey);
    }
    knownAddresses.add(publicAddress);

    ownedAddresses.get(user).add(publicAddress);

    LOGGER.debug("New address " + publicAddress + " generated after " + rounds + " rounds");
    return publicAddress;
  }

  @Override
  public boolean registerAddress(String address) {
    return true;
  }

  @Override
  public String createAddressFromKey(String key, boolean isPrivateKey) {
    return EthereumTools.getPublicAddress(key, isPrivateKey);
  }

  @Override
  public Iterable<String> getAddresses(String name) {
    String user = EthereumTools.encodeUserKey(name);
    if (!ownedAddresses.containsKey(user)) {
      ownedAddresses.put(user, new HashSet<>());
    }

    HashSet<String> userAddresses = ownedAddresses.get(user);
    if (userAddresses.isEmpty()) {
      String balanceCheck = getBalance(createAddress(name));
      while (balanceCheck != null && balanceCheck != BigDecimal.ZERO.toPlainString()) {
        userAddresses = ownedAddresses.get(user);
        balanceCheck = getBalance(createAddress(name));
      }
    }

    return userAddresses;
  }

  @Override
  public String getMultiSigAddress(Iterable<String> addresses, String name) {
    return addresses.iterator().next();
  }

  @Override
  public String getBalance(String address) {
    CallData callData = new CallData();
    callData.setTo("0x" + contractAddress);
    callData.setData("0x" + contractInterface.getContractParameters().getBalance(address));
    callData.setGas("100000"); // Doesn't matter, just can't be nil
    callData.setGasPrice("100000"); // Doesn't matter, just can't be nil
    String response = ethereumRpc.eth_call(callData, DefaultBlock.LATEST.toString());

    BigInteger balance = new BigInteger(1, ByteUtilities.toByteArray(response));
    return balance.toString(10);
  }

  @Override
  public String createTransaction(Iterable<String> fromAddresses, Iterable<Recipient> toAddresses) {
    // Create the TX data structure
    RawTransaction tx = new RawTransaction();
    tx.getTo().setDecodedContents(ByteUtilities.toByteArray(contractAddress));
    tx.getGasPrice().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
    tx.getGasLimit().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));

    List<String> recipients = new LinkedList<>();
    List<Long> amounts = new LinkedList<>();
    toAddresses.forEach(recipient -> {
      amounts.add(recipient.getAmount().longValue());
      recipients.add(recipient.getRecipientAddress());
    });

    String txCount = ethereumRpc
        .eth_getStorageAt("0x" + contractAddress.toLowerCase(Locale.US), "0x1",
            DefaultBlock.LATEST.toString());
    BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount)).add(BigInteger.ONE);

    tx.getData().setDecodedContents(ByteUtilities.toByteArray(
        contractInterface.getContractParameters()
            .transfer(nonce.longValue(), fromAddresses.iterator().next(), recipients, amounts,
                new LinkedList<>(), new LinkedList<>(), new LinkedList<>())));

    return ByteUtilities.toHexString(tx.encode());
  }

  @Override
  public Iterable<String> getSignersForTransaction(String transaction) {
    RawTransaction rawTx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    String contractData = ByteUtilities.toHexString(rawTx.getData().getDecodedContents());
    Map<String, List<String>> contractDataParams =
        contractInterface.getContractParameters().parseTransfer(contractData);

    return contractDataParams.get(FiatContractParametersInterface.SENDER);
  }

  @Override
  public String signTransaction(String transaction, String address) {
    return signTransaction(transaction, address, null);
  }

  @Override
  public String signTransaction(String transaction, String address, String name) {
    Iterable<Iterable<String>> sigData = getSigString(transaction, address);
    sigData = signWithPrivateKey(sigData, name, address);
    return applySignature(transaction, address, sigData);
  }

  @Override
  public Iterable<Iterable<String>> getSigString(String transaction, String address) {
    return getSigString(transaction, address, false);
  }

  public Iterable<Iterable<String>> getSigString(String transaction, String address,
      boolean ignoreContractCode) {
    RawTransaction tx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    LinkedList<Iterable<String>> sigStrings = new LinkedList<>();

    if (!ignoreContractCode && ByteUtilities.toHexString(tx.getTo().getDecodedContents())
        .equalsIgnoreCase(contractAddress)) {
      // Initialize hash (IV = 0x00)
      String hashBytes = String.format("%64s", "0").replace(' ', '0');

      // Get the transaction data
      Map<String, List<String>> contractParams =
          contractInterface.getContractParameters().parseTransfer(transaction);
      BigInteger nonce =
          new BigInteger(contractParams.get(FiatContractParametersInterface.NONCE).get(0));
      List<String> recipients = contractParams.get(FiatContractParametersInterface.RECIPIENTS);
      List<String> amounts = contractParams.get(FiatContractParametersInterface.AMOUNT);

      // Hash to sign is hash(previous hash + recipient + amount + nonce)
      for (int i = 0; i < recipients.size(); i++) {
        hashBytes += String.format("%40s", recipients.get(i)).replace(' ', '0');
        hashBytes += String
            .format("%40s", ByteUtilities.toHexString(new BigInteger(amounts.get(i)).toByteArray()))
            .replace(' ', '0');
        hashBytes +=
            String.format("%40s", ByteUtilities.toHexString(nonce.toByteArray())).replace(' ', '0');

        LOGGER.debug("Hashing: " + hashBytes);
        hashBytes = EthereumTools.hashKeccak(hashBytes);
        LOGGER.debug("Result: " + hashBytes);
      }
      LinkedList<String> msigString = new LinkedList<>();
      msigString.add(contractInterface.getClass().getCanonicalName());
      msigString.add(hashBytes);
      sigStrings.add(msigString);
    }

    // Calculate the transaction's signature data.
    String txCount =
        ethereumRpc.eth_getTransactionCount("0x" + address, DefaultBlock.LATEST.toString());
    LinkedList<String> txString = new LinkedList<>();
    txString.add(transaction);
    txString.add(txCount);
    sigStrings.add(txString);

    LOGGER.debug(sigStrings.toString());

    return sigStrings;
  }

  @Override
  public String applySignature(String transaction, String address,
      Iterable<Iterable<String>> signatureData) {
    // This is taken care of in the signing process for Ethereum, so we can just return the data.
    try {
      return signatureData.iterator().next().iterator().next();
    } catch (Exception e) {
      return "";
    }
  }

  @Override
  public String sendTransaction(String transaction) {
    Iterable<Iterable<String>> sigData =
        getSigString(transaction, config.getContractAccount(), true);
    sigData = signWithPrivateKey(sigData, null, config.getContractAccount());
    transaction = applySignature(transaction, config.getContractAccount(), sigData);

    return ethereumRpc.eth_sendRawTransaction(transaction);
  }

  @Override
  public TransactionDetails[] getTransactions(String address, int numberToReturn, int skipNumber) {
    // TODO
    return new TransactionDetails[0];
  }

  @Override
  public ServerStatus getWalletStatus() {
    try {
      ethereumRpc.eth_blockNumber();
      return ServerStatus.CONNECTED;
    } catch (Exception e) {
      return ServerStatus.DISCONNECTED;
    }
  }

  @Override
  public String generatePrivateKey() {
    return ByteUtilities.toHexString(Secp256k1.generatePrivateKey());
  }

  @Override
  public String generatePublicKey(String privateKey) {
    return EthereumTools.getPublicKey(privateKey);
  }

  @Override
  public Iterable<Iterable<String>> signWithPrivateKey(Iterable<Iterable<String>> data,
      String privateKey) {
    return signWithPrivateKey(data, privateKey, null);
  }

  private Iterable<Iterable<String>> signWithPrivateKey(Iterable<Iterable<String>> data,
      String privateKey, String address) {
    LOGGER.debug("Attempting to sign: " + address + data.toString());

    LinkedList<Iterable<String>> signedData = new LinkedList<>();
    LinkedList<Iterable<String>> listedData = new LinkedList<>();
    data.forEach(listedData::add);
    LinkedList<String> contractData = new LinkedList<>();
    LinkedList<String> txData = new LinkedList<>();
    // Check if there are two entries, if there are, the first one should be mSig data.
    int txDataLocation = 0;
    if (listedData.size() == 2) {
      txDataLocation++;
      listedData.get(0).forEach(contractData::add);
    }
    listedData.get(txDataLocation).forEach(txData::add);

    try {
      // Sign mSig if there is any
      if (contractData.size() > 0) {
        LOGGER.debug("Reading mSig data");
        String sigBytes = contractData.getLast();
        byte[][] sigData = signData(sigBytes, address, privateKey);
        // Return the original TX on failure
        if (sigData.length < 3) {
          LinkedList<String> signature = new LinkedList<>();
          signature.add(txData.getFirst());
          LinkedList<Iterable<String>> result = new LinkedList<>();
          result.add(signature);
          return result;
        }

        LinkedList<String> msigSig = new LinkedList<>();
        msigSig.add(ByteUtilities.toHexString(sigData[0]));
        msigSig.add(ByteUtilities.toHexString(sigData[1]));
        msigSig.add(ByteUtilities.toHexString(sigData[2]));
        signedData.add(msigSig);
      } else {
        LOGGER.debug("No mSig data to process.");
      }
      // Rebuild the TX if there is any mSig data
      RawTransaction rawTx =
          RawTransaction.parseBytes(ByteUtilities.toByteArray(txData.getFirst()));
      // If we've added mSig data then update the TX.
      if (signedData.size() > 0) {
        String contractVersion = contractData.getFirst();
        FiatContractInterface contract =
            (FiatContractInterface) FiatContractInterface.class.getClassLoader()
                .loadClass(contractVersion).newInstance();

        FiatContractParametersInterface contractParms = contract.getContractParameters();
        Map<String, List<String>> contractParamData = contractParms
            .parseTransfer(ByteUtilities.toHexString(rawTx.getData().getDecodedContents()));

        Iterator<String> msigSig = signedData.getFirst().iterator();
        contractParamData.get(FiatContractParametersInterface.SIGR).add(msigSig.next());
        contractParamData.get(FiatContractParametersInterface.SIGS).add(msigSig.next());
        contractParamData.get(FiatContractParametersInterface.SIGV).add(msigSig.next());

        Long nonce =
            new BigInteger(contractParamData.get(FiatContractParametersInterface.NONCE).get(0))
                .longValue();
        String sender = contractParamData.get(FiatContractParametersInterface.SENDER).get(0);
        List<String> recipients = contractParamData.get(FiatContractParametersInterface.RECIPIENTS);
        List<Long> amounts = new LinkedList<>();
        for (String amount : contractParamData.get(FiatContractParametersInterface.AMOUNT)) {
          amounts.add(new BigInteger(amount).longValue());
        }
        List<String> sigV = contractParamData.get(FiatContractParametersInterface.SIGV);
        List<String> sigR = contractParamData.get(FiatContractParametersInterface.SIGR);
        List<String> sigS = contractParamData.get(FiatContractParametersInterface.SIGS);

        rawTx.getData().setDecodedContents(ByteUtilities.toByteArray(
            contractParms.transfer(nonce, sender, recipients, amounts, sigV, sigR, sigS)));
      }
      // Sign the TX
      String txCount = txData.getLast();
      BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount));

      if (nonce.equals(BigInteger.ZERO)) {
        rawTx.getNonce().setDecodedContents(new byte[]{});
      } else {
        rawTx.getNonce()
            .setDecodedContents(ByteUtilities.stripLeadingNullBytes(nonce.toByteArray()));
      }

      String sigString = ByteUtilities.toHexString(rawTx.getSigBytes());
      LOGGER.debug("Tx: " + ByteUtilities.toHexString(rawTx.encode()));
      LOGGER.debug("SigBytes: " + sigString);
      sigString = EthereumTools.hashKeccak(sigString);
      LOGGER.debug("Hashed: " + sigString);
      byte[][] sigData = signData(sigString, address, privateKey);
      if (sigData.length < 3) {
        LinkedList<String> signature = new LinkedList<>();
        signature.add(txData.getFirst());
        LinkedList<Iterable<String>> result = new LinkedList<>();
        result.add(signature);
        return result;
      }

      rawTx.getSigR().setDecodedContents(sigData[0]);
      rawTx.getSigS().setDecodedContents(sigData[1]);
      rawTx.getSigV().setDecodedContents(sigData[2]);

      // Return the signed TX as-is, we don't need network information to apply it.
      LinkedList<String> signature = new LinkedList<>();
      signature.add(ByteUtilities.toHexString(rawTx.encode()));
      LinkedList<Iterable<String>> result = new LinkedList<>();
      result.add(signature);
      result.add(new LinkedList<>(Collections.singletonList(sigString)));
      return result;
    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
      LOGGER.warn(null, e);
      LinkedList<String> signature = new LinkedList<>();
      signature.add(txData.getFirst());
      LinkedList<Iterable<String>> result = new LinkedList<>();
      result.add(signature);
      return result;
    }
  }

  private byte[][] signData(String data, String address, String name) {
    if (name == null) {
      // Catch errors here
      String sig;
      try {
        LOGGER.debug("Asking geth to sign " + data + " for 0x" + address);
        sig = ethereumRpc.eth_sign("0x" + address, data);
      } catch (Exception e) {
        LOGGER.warn(null, e);
        return new byte[0][0];
      }

      try {
        LOGGER.debug("Decoding sig result: " + sig);
        byte[] sigBytes = ByteUtilities.toByteArray(sig);
        byte[] sigR = Arrays.copyOfRange(sigBytes, 0, 32);
        byte[] sigS = Arrays.copyOfRange(sigBytes, 32, 64);
        byte[] sigV = Arrays.copyOfRange(sigBytes, 64, 65);

        String signingAddress = null;
        try {
          signingAddress = ByteUtilities.toHexString(
              Secp256k1.recoverPublicKey(sigR, sigS, sigV, ByteUtilities.toByteArray(data)))
              .substring(2);
        } catch (Exception e) {
          LOGGER.debug("Couldn't recover public key from signature", e);
        }
        signingAddress = EthereumTools.getPublicAddress(signingAddress, false);
        LOGGER.debug("Appears to be signed by: " + signingAddress);

        // Adjust for expected format.
        sigV[0] += 27;

        return new byte[][]{sigR, sigS, sigV};
      } catch (Exception e) {
        LOGGER.error(null, e);
        return new byte[0][0];
      }
    } else {
      int maxRounds = 1000;

      String privateKey = "";
      if (address != null) {
        for (int i = 1; i <= maxRounds; i++) {
          String privateKeyCheck =
              EthereumTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), i);
          if (EthereumTools.getPublicAddress(privateKeyCheck).equalsIgnoreCase(address)) {
            privateKey = privateKeyCheck;
            break;
          }
        }
        if (privateKey.isEmpty()) {
          return new byte[0][0];
        }
      } else {
        privateKey = name;
        address = EthereumTools.getPublicAddress(privateKey);
      }

      // Sign and return it
      byte[] privateBytes = ByteUtilities.toByteArray(privateKey);
      byte[] sigBytes = ByteUtilities.toByteArray(data);
      String signingAddress = "";

      // The odd signature can't be resolved to a recoveryId, in those cases, just sign it again.
      byte[] sigV;
      byte[] sigR;
      byte[] sigS;
      do {
        byte[][] signedBytes = Secp256k1.signTransaction(sigBytes, privateBytes);
        sigR = ByteUtilities.stripLeadingNullBytes(signedBytes[0]);
        sigS = ByteUtilities.stripLeadingNullBytes(signedBytes[1]);
        sigV = signedBytes[2];

        if (sigV[0] != 0 && sigV[0] != 1) {
          continue;
        }

        try {
          signingAddress =
              ByteUtilities.toHexString(Secp256k1.recoverPublicKey(sigR, sigS, sigV, sigBytes))
                  .substring(2);
        } catch (Exception e) {
          LOGGER.debug("Couldn't recover the public key", e);
        }
        signingAddress = EthereumTools.getPublicAddress(signingAddress, false);
      } while (!address.equalsIgnoreCase(signingAddress));

      // Adjust for ethereum's encoding
      sigV[0] += 27;

      return new byte[][]{sigR, sigS, sigV};
    }
  }

  // TODO Add some admin functions for creation and destruction of tokens. Figure out where we're going to put these in tooling.
}
