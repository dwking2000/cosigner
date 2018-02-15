package io.emax.cosigner.ethereum.core;

import io.emax.cosigner.api.core.ServerStatus;
import io.emax.cosigner.api.currency.CurrencyAdmin;
import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.api.validation.Validatable;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.common.crypto.Secp256k1;
import io.emax.cosigner.ethereum.core.common.EthereumTools;
import io.emax.cosigner.ethereum.core.common.RlpItem;
import io.emax.cosigner.ethereum.core.common.RlpList;
import io.emax.cosigner.ethereum.core.gethrpc.Block;
import io.emax.cosigner.ethereum.core.gethrpc.CallData;
import io.emax.cosigner.ethereum.core.gethrpc.DefaultBlock;
import io.emax.cosigner.ethereum.core.gethrpc.EthereumRpc;
import io.emax.cosigner.ethereum.core.gethrpc.RawTransaction;
import io.emax.cosigner.ethereum.core.gethrpc.multisig.ContractInformation;
import io.emax.cosigner.ethereum.core.gethrpc.multisig.MultiSigContract;
import io.emax.cosigner.ethereum.core.gethrpc.multisig.MultiSigContractInterface;
import io.emax.cosigner.ethereum.core.gethrpc.multisig.MultiSigContractParametersInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EthereumWallet implements Wallet, Validatable, CurrencyAdmin {
  private static final Logger LOGGER = LoggerFactory.getLogger(EthereumWallet.class);
  private static final String TESTNET_VERSION = "2";
  private static final long TESTNET_BASE_ROUNDS = (long) Math.pow(2, 20);

  private final EthereumRpc ethereumWriteRpc = EthereumResource.getResource().getEthWriteRPC();
  private final EthereumRpc ethereumReadRpc = EthereumResource.getResource().getEthReadRPC();
  EthereumConfiguration config;

  private final HashMap<String, Integer> addressRounds = new HashMap<>();
  private final HashMap<String, ContractInformation> msigContracts = new HashMap<>();
  private final HashMap<String, String> reverseMsigContracts = new HashMap<>();

  private Thread multiSigSubscription = new Thread(() -> {
    while (true) {
      try {
        LOGGER.info("Scanning ETH multi-sig addresses");
        syncMultiSigAddresses();
        Thread.sleep(60000);
      } catch (Exception e) {
        LOGGER.debug("Multisig scan interrupted.");
      }
    }
  });

  public EthereumWallet(EthereumConfiguration conf) {
    this.config = conf;
    try {
      syncMultiSigAddresses();
    } catch (Exception e) {
      LOGGER.debug(null, e);
    }

    if (!multiSigSubscription.isAlive()) {
      multiSigSubscription.setDaemon(true);
      multiSigSubscription.start();
    }
  }

  private volatile boolean synching = false;

  private void syncMultiSigAddresses() {
    if (synching) {
      return;
    }
    try {
      synching = true;
      String decodedContractAccount = config.getContractAccount();
      String contractKey = config.getContractKey();

      if (!contractKey.isEmpty()) {
        decodedContractAccount = EthereumTools.getPublicAddress(contractKey, true);
      }

      if (decodedContractAccount == null || decodedContractAccount.isEmpty()) {
        synching = false;
        return;
      }

      LOGGER.info(
          "Synchronizing contract accounts with network... (0x" + decodedContractAccount + ")");
      String txCount = ethereumReadRpc
          .eth_getTransactionCount("0x" + decodedContractAccount, DefaultBlock.LATEST.toString());
      int rounds = new BigInteger(1, ByteUtilities.toByteArray(txCount)).intValue();
      int baseRounds = 0;
      if (ethereumReadRpc.net_version().equals(TESTNET_VERSION)) {
        baseRounds = (int) TESTNET_BASE_ROUNDS;
      }

      for (int i = baseRounds; i < rounds; i++) {
        if (i % 50000 == 0) {
          LOGGER.info("Scanning ETH: " + i + "/" + rounds + "...");
        }
        RlpList contractAddress = new RlpList();
        RlpItem contractCreator = new RlpItem(ByteUtilities.toByteArray(decodedContractAccount));
        RlpItem nonce =
            new RlpItem(ByteUtilities.stripLeadingNullBytes(BigInteger.valueOf(i).toByteArray()));
        contractAddress.add(contractCreator);
        contractAddress.add(nonce);

        String contract =
            EthereumTools.hashKeccak(ByteUtilities.toHexString(contractAddress.encode()))
                .substring(96 / 4, 256 / 4);
        String contractCode = ethereumReadRpc
            .eth_getCode("0x" + contract.toLowerCase(Locale.US), DefaultBlock.LATEST.toString());

        contractCode = contractCode.substring(2);
        Class<?> contractType = MultiSigContract.class;
        while (MultiSigContractInterface.class.isAssignableFrom(contractType)) {
          MultiSigContractInterface contractParams =
              (MultiSigContractInterface) contractType.newInstance();
          if (contractParams.getContractPayload().equalsIgnoreCase(contractCode)) {
            try {
              // Found an existing contract
              LOGGER.debug("Found existing contract version: " + contractType.getCanonicalName());
              CallData callData = new CallData();
              callData.setTo("0x" + contract);
              callData.setValue("0");
              callData.setData("0x" + contractParams.getGetOwnersFunctionAddress());
              callData.setGas("100000"); // Doesn't matter, just can't be nil
              callData.setGasPrice("100000"); // Doesn't matter, just can't be nil
              String response = ethereumReadRpc.eth_call(callData, DefaultBlock.LATEST.toString());

              // Gather addresses
              byte[] callBytes = ByteUtilities.toByteArray(response);
              int bufferPointer = 32; // skip first value, we know it just points to the next one.
              byte[] sizeBytes = Arrays.copyOfRange(callBytes, bufferPointer, bufferPointer + 32);
              bufferPointer += 32;
              int numAddresses = new BigInteger(1, sizeBytes).intValue();
              for (int j = 0; j < numAddresses; j++) {
                byte[] addressBytes =
                    Arrays.copyOfRange(callBytes, bufferPointer, bufferPointer + 32);
                bufferPointer += 32;
                String userAddress =
                    ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(addressBytes));
                userAddress = String.format("%40s", userAddress).replace(' ', '0');

                if (reverseMsigContracts.containsKey(contract.toLowerCase(Locale.US))) {
                  continue;
                }

                msigContracts.put(userAddress.toLowerCase(Locale.US),
                    new ContractInformation(contract.toLowerCase(Locale.US), contractCode,
                        contractType));
                reverseMsigContracts
                    .put(contract.toLowerCase(Locale.US), userAddress.toLowerCase(Locale.US));
              }
              break;
            } catch (Exception e) {
              LOGGER.warn("Could not process contract data for contract at " + contract + "!");
              LOGGER.trace(null, e);
              break;
            }
          }

          contractType = contractType.getSuperclass();
        }
      }
    } catch (Exception e) {
      LOGGER.warn("Error scanning for existing contracts!");
    } finally {
      synching = false;
    }
  }

  @Override
  public String createAddress(String name) {
    return createAddress(name, 0);
  }

  @Override
  public String createAddress(String name, int skipNumber) {
    LOGGER.debug("Creating a new normal address...");
    int rounds = 1 + skipNumber;
    String privateKey =
        EthereumTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);

    String publicAddress = EthereumTools.getPublicAddress(privateKey);

    while (msigContracts.containsKey(publicAddress.toLowerCase(Locale.US))) {
      rounds++;
      privateKey =
          EthereumTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);
      publicAddress = EthereumTools.getPublicAddress(privateKey);
    }
    addressRounds.put(name, rounds);

    LOGGER.debug("New address " + publicAddress + " generated after " + rounds + " rounds");
    return publicAddress;
  }

  @Override
  public boolean registerAddress(String address) {
    return true;
  }

  @Override
  public String generatePrivateKey() {
    return ByteUtilities.toHexString(Secp256k1.generatePrivateKey());
  }

  @Override
  public String createAddressFromKey(String key, boolean isPrivateKey) {
    return EthereumTools.getPublicAddress(key, isPrivateKey);
  }

  @Override
  public String generatePublicKey(String privateKey) {
    return EthereumTools.getPublicKey(privateKey);
  }

  @Override
  public Iterable<String> getAddresses(String name) {
    int maxRounds;
    if (addressRounds.containsKey(name)) {
      maxRounds = addressRounds.get(name);
    } else {
      createAddress(name);
      maxRounds = addressRounds.get(name);
    }

    LinkedList<String> addresses = new LinkedList<>();
    for (int i = 1; i <= maxRounds; i++) {
      addresses.add(EthereumTools.getPublicAddress(
          EthereumTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), i)));
    }

    LinkedList<String> contracts = new LinkedList<>();
    for (String address : addresses) {
      if (msigContracts.containsKey(address.toLowerCase(Locale.US))) {
        contracts.add(msigContracts.get(address.toLowerCase(Locale.US)).getContractAddress());
      }
    }

    return contracts;
  }

  @Override
  public String getMultiSigAddress(Iterable<String> addresses, String name) throws Exception {
    String userAddress = "";
    Iterator<String> addIter = addresses.iterator();
    List<String> addressesUsed = new LinkedList<>();
    while (addIter.hasNext()) {
      String addr = addIter.next();
      userAddress += addr.toLowerCase(Locale.US);
      addressesUsed.add(String.format("%64s", addr).replace(' ', '0'));
    }
    LOGGER.debug("userAddress: " + userAddress);

    if (msigContracts.containsKey(userAddress.toLowerCase(Locale.US))) {
      LOGGER.debug("Found existing address: " + msigContracts.get(userAddress).getContractAddress()
          .toLowerCase(Locale.US));
      return msigContracts.get(userAddress).getContractAddress().toLowerCase(Locale.US);
    }

    LOGGER.debug("Generating new contract...");
    RawTransaction tx = new RawTransaction();
    tx.getGasPrice().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
    tx.getGasLimit().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));

    final String contractInit = new MultiSigContract().getInitData();
    final String accountOffset = String.format("%64s", "40").replace(' ', '0');
    String requiredSigs =
        ByteUtilities.toHexString(BigInteger.valueOf(config.getMinSignatures()).toByteArray());
    requiredSigs = String.format("%64s", requiredSigs).replace(' ', '0');

    for (int i = 0; i < config.getMultiSigAddresses().length; i++) {
      if (config.getMultiSigAddresses()[i].isEmpty()) {
        continue;
      }
      addressesUsed.add(String.format("%64s", config.getMultiSigAddresses()[i]).replace(' ', '0'));
    }
    for (int i = 0; i < config.getMultiSigKeys().length; i++) {
      if (config.getMultiSigKeys()[i].isEmpty()) {
        continue;
      }
      String convertedAddress = EthereumTools.getPublicAddress(config.getMultiSigKeys()[i], true);
      addressesUsed.add(String.format("%64s", convertedAddress).replace(' ', '0'));
    }

    String numberOfAddresses =
        ByteUtilities.toHexString(BigInteger.valueOf(addressesUsed.size()).toByteArray());
    numberOfAddresses = String.format("%64s", numberOfAddresses).replace(' ', '0');

    StringBuilder contractCode = new StringBuilder();
    contractCode.append(contractInit).append(accountOffset).append(requiredSigs)
        .append(numberOfAddresses);
    for (int i = 0; i < addressesUsed.size(); i++) {
      contractCode.append(addressesUsed.get(i));
      LOGGER.debug("Adding address to signers: " + addressesUsed.get(i));
    }
    tx.getData().setDecodedContents(ByteUtilities.toByteArray(contractCode.toString()));

    // Sign it with our contract creator, creator needs funds to pay for the creation
    String rawTx = ByteUtilities.toHexString(tx.encode());
    String decodedContractAddress = config.getContractAccount();
    String contractKey = config.getContractKey();

    if (!contractKey.isEmpty()) {
      decodedContractAddress = EthereumTools.getPublicAddress(contractKey, true);
    } else {
      contractKey = null;
    }
    LOGGER.debug("Attempting to create contract with account: " + decodedContractAddress);
    Iterable<Iterable<String>> sigData = getSigString(rawTx, decodedContractAddress, false);
    sigData = signTx(sigData, contractKey == null ? decodedContractAddress : null, contractKey);
    String signedTx = applySignature(rawTx, decodedContractAddress, sigData);

    if (signedTx.equalsIgnoreCase(rawTx)) {
      return "";
    }

    tx = RawTransaction.parseBytes(ByteUtilities.toByteArray(signedTx));
    RlpList contractAddress = new RlpList();
    RlpItem contractCreator = new RlpItem(ByteUtilities.toByteArray(decodedContractAddress));
    contractAddress.add(contractCreator);
    contractAddress.add(tx.getNonce());

    String contract = EthereumTools.hashKeccak(ByteUtilities.toHexString(contractAddress.encode()))
        .substring(96 / 4, 256 / 4);

    LOGGER.debug("Expecting new contract address of " + contract + " with tx: " + RawTransaction
        .parseBytes(ByteUtilities.toByteArray(signedTx)));

    if (reverseMsigContracts.containsKey(contract.toLowerCase(Locale.US))) {
      return "";
    }

    sendTransaction(signedTx);

    msigContracts.put(userAddress, new ContractInformation(contract.toLowerCase(Locale.US),
        new MultiSigContract().getContractPayload(), MultiSigContract.class));
    reverseMsigContracts.put(contract.toLowerCase(Locale.US), userAddress);

    return contract;
  }

  @Override
  public String getBalance(String address) throws Exception {
    address = "0x" + ByteUtilities.toHexString(ByteUtilities.toByteArray(address));
    BigInteger latestBlockNumber =
        new BigInteger("00" + ethereumReadRpc.eth_blockNumber().substring(2), 16);
    BigInteger confirmedBlockNumber =
        latestBlockNumber.subtract(BigInteger.valueOf(config.getMinConfirmations()));

    BigInteger latestBalance = new BigInteger(
        "00" + ethereumReadRpc.eth_getBalance(address, "0x" + latestBlockNumber.toString(16))
            .substring(2), 16);
    BigInteger confirmedBalance = new BigInteger(
        "00" + ethereumReadRpc.eth_getBalance(address, "0x" + confirmedBlockNumber.toString(16))
            .substring(2), 16);

    confirmedBalance = confirmedBalance.min(latestBalance);
    BigDecimal etherBalance = new BigDecimal(confirmedBalance);
    etherBalance = etherBalance.setScale(20, BigDecimal.ROUND_UNNECESSARY);
    etherBalance = etherBalance
        .divide(BigDecimal.valueOf(config.getWeiMultiplier()), BigDecimal.ROUND_UNNECESSARY);
    return etherBalance.toPlainString();
  }

  @Override
  public String getPendingBalance(String address) throws Exception {
    address = "0x" + ByteUtilities.toHexString(ByteUtilities.toByteArray(address));
    BigInteger latestBlockNumber =
        new BigInteger("00" + ethereumReadRpc.eth_blockNumber().substring(2), 16);
    BigInteger confirmedBlockNumber =
        latestBlockNumber.subtract(BigInteger.valueOf(config.getMinConfirmations()));

    BigInteger latestBalance = new BigInteger(
        "00" + ethereumReadRpc.eth_getBalance(address, "0x" + latestBlockNumber.toString(16))
            .substring(2), 16);
    BigInteger confirmedBalance = new BigInteger(
        "00" + ethereumReadRpc.eth_getBalance(address, "0x" + confirmedBlockNumber.toString(16))
            .substring(2), 16);

    latestBalance = latestBalance.subtract(confirmedBalance).max(BigInteger.ZERO);
    BigDecimal etherBalance = new BigDecimal(latestBalance);
    etherBalance = etherBalance.setScale(20, BigDecimal.ROUND_UNNECESSARY);
    etherBalance = etherBalance
        .divide(BigDecimal.valueOf(config.getWeiMultiplier()), BigDecimal.ROUND_UNNECESSARY);
    return etherBalance.toPlainString();
  }

  @Override
  public String createTransaction(Iterable<String> fromAddresses, Iterable<Recipient> toAddresses)
      throws Exception {
    return createTransaction(fromAddresses, toAddresses, null);
  }

  @Override
  public String createTransaction(Iterable<String> fromAddress, Iterable<Recipient> toAddress,
      String options) throws Exception {

    String senderAddress = fromAddress.iterator().next();
    boolean isMsigSender = false;
    LinkedList<String> possibleSenders = new LinkedList<>();
    msigContracts.forEach((user, msig) -> {
      if (msig.getContractAddress().equalsIgnoreCase(senderAddress)) {
        possibleSenders.add(user);
      }
    });
    if (!possibleSenders.isEmpty()) {
      isMsigSender = true;
    }

    Recipient recipient = toAddress.iterator().next();

    BigDecimal amountWei =
        recipient.getAmount().multiply(BigDecimal.valueOf(config.getWeiMultiplier()));

    RawTransaction tx = new RawTransaction();
    tx.getGasPrice().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
    tx.getGasLimit().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getSimpleTxGas()).toByteArray()));
    tx.getTo().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(new BigInteger(recipient.getRecipientAddress(), 16).toByteArray()));
    tx.getValue().setDecodedContents(
        ByteUtilities.stripLeadingNullBytes(amountWei.toBigInteger().toByteArray()));

    try {
      if (isMsigSender) {
        tx.getGasLimit().setDecodedContents(ByteUtilities
            .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));

        tx.getValue().setDecodedContents(new byte[]{});

        MultiSigContractInterface contract =
            (MultiSigContractInterface) msigContracts.get(possibleSenders.getFirst())
                .getContractVersion().newInstance();
        LOGGER.debug(
            "Creating transaction for contract version: " + contract.getClass().getCanonicalName());
        MultiSigContractParametersInterface contractParms = contract.getContractParameters();
        contractParms.setFunction(contract.getExecuteFunctionAddress());
        toAddress.iterator().forEachRemaining(rcpt -> {
          contractParms.getAddress().add(rcpt.getRecipientAddress());
          contractParms.getValue().add(
              rcpt.getAmount().multiply(BigDecimal.valueOf(config.getWeiMultiplier()))
                  .toBigInteger());
        });

        String txCount = ethereumReadRpc
            .eth_getStorageAt("0x" + senderAddress.toLowerCase(Locale.US), "0x1",
                DefaultBlock.LATEST.toString());
        BigInteger nonce =
            new BigInteger(1, ByteUtilities.toByteArray(txCount)).add(BigInteger.ONE);
        contractParms.setNonce(nonce);

        tx.getData().setDecodedContents(contractParms.encode());

        tx.getTo().setDecodedContents(
            ByteUtilities.stripLeadingNullBytes(new BigInteger(senderAddress, 16).toByteArray()));
      }
    } catch (InstantiationException | IllegalAccessException e) {
      LOGGER.error(null, e);
    }

    return ByteUtilities.toHexString(tx.encode());
  }

  @Override
  public Iterable<String> getSignersForTransaction(String transaction) {
    RawTransaction tx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    String contractAddress =
        ByteUtilities.toHexString(tx.getTo().getDecodedContents()).toLowerCase(Locale.US);

    // We can only give information on the contracts we monitor.
    if (reverseMsigContracts.containsKey(contractAddress)) {
      try {
        String userAddress = reverseMsigContracts.get(contractAddress);
        ContractInformation contractInfo = msigContracts.get(userAddress);
        MultiSigContractInterface contract =
            (MultiSigContractInterface) contractInfo.getContractVersion().newInstance();

        MultiSigContractParametersInterface contractParams = contract.getContractParameters();
        contractParams.decode(tx.getData().getDecodedContents());

        CallData callData = new CallData();
        callData.setTo("0x" + contractAddress);
        callData.setValue("0");
        callData.setData("0x" + contract.getGetOwnersFunctionAddress());
        callData.setGas("100000"); // Doesn't matter, just can't be nil
        callData.setGasPrice("100000"); // Doesn't matter, just can't be nil
        String response = ethereumReadRpc.eth_call(callData, DefaultBlock.LATEST.toString());

        LinkedList<String> addresses = new LinkedList<>();
        byte[] callBytes = ByteUtilities.toByteArray(response);
        int bufferPointer = 32; // skip first value, it just points to the next one.
        byte[] sizeBytes = Arrays.copyOfRange(callBytes, bufferPointer, bufferPointer + 32);
        bufferPointer += 32;
        int numAddresses = new BigInteger(1, sizeBytes).intValue();
        for (int j = 0; j < numAddresses; j++) {
          byte[] addressBytes = Arrays.copyOfRange(callBytes, bufferPointer, bufferPointer + 32);
          bufferPointer += 32;
          userAddress =
              ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(addressBytes));
          userAddress = String.format("%40s", userAddress).replace(' ', '0');
          addresses.add(userAddress);
        }

        return addresses;
      } catch (Exception e) {
        LOGGER.debug("Something went wrong with decoding the contract.");
        LOGGER.trace(null, e);
        return new LinkedList<String>();
      }
    } else {
      return new LinkedList<String>();
    }
  }

  @Override
  public String signTransaction(String transaction, String address) throws Exception {
    return signTransaction(transaction, address, null);
  }

  @Override
  public String signTransaction(String transaction, String address, String name) throws Exception {
    return signTransaction(transaction, address, name, null);
  }

  @Override
  public String signTransaction(String transaction, String address, String name, String options)
      throws Exception {
    LOGGER.debug("Attempting to sign for address: " + address);
    TransactionDetails txDetails = decodeRawTransaction(transaction);
    String sender = address;
    if (reverseMsigContracts.containsKey(address.toLowerCase(Locale.US))) {
      sender = txDetails.getFromAddress()[0];
    }
    if (txDetails.getAmount().compareTo(new BigDecimal(getBalance(sender))) > 0) {
      LOGGER.debug("Refusing to sign, account does not have enough balance.");
      LOGGER.debug("Amount sent: " + txDetails.getAmount()
          .divide(BigDecimal.valueOf(config.getWeiMultiplier())).toPlainString());
      LOGGER.debug("Balance available (" + sender + "): " + (new BigDecimal(
          getBalance(txDetails.getFromAddress()[0])).toPlainString()));
      return transaction;
    }

    // Prepare sigData so that if we can't sign, it returns the original.
    LinkedList<String> txData = new LinkedList<>();
    txData.add(transaction);
    LinkedList<Iterable<String>> wrappedTxdata = new LinkedList<>();
    wrappedTxdata.add(txData);
    Iterable<Iterable<String>> sigData = wrappedTxdata;

    if (name == null && reverseMsigContracts.containsKey(address.toLowerCase(Locale.US))) {
      LOGGER.debug("Attempting to sign with multi-sig keys");
      for (int i = 0; i < config.getMultiSigAddresses().length; i++) {
        if (config.getMultiSigAddresses()[i].isEmpty()) {
          continue;
        }
        sigData = getSigString(transaction, config.getMultiSigAddresses()[i]);
        sigData = signTx(sigData, config.getMultiSigAddresses()[i], null);
        transaction = applySignature(transaction, address, sigData);
      }
      for (int i = 0; i < config.getMultiSigKeys().length; i++) {
        if (config.getMultiSigKeys()[i].isEmpty()) {
          continue;
        }
        String msigAddress = EthereumTools.getPublicAddress(config.getMultiSigKeys()[i], true);
        sigData = getSigString(transaction, msigAddress);
        sigData = signTx(sigData, null, config.getMultiSigKeys()[i]);
        transaction = applySignature(transaction, address, sigData);
      }
    } else if (name == null) {
      LOGGER.debug("Attempting to sign with 3rd party signer");
      sigData = getSigString(transaction, address);
      sigData = signTx(sigData, address, null);
      transaction = applySignature(transaction, address, sigData);
    } else if (reverseMsigContracts.containsKey(address.toLowerCase(Locale.US))) {
      LOGGER.debug("Attempting to sign with userKey, translated contract address");
      String translatedAddress = reverseMsigContracts.get(address.toLowerCase(Locale.US));
      sigData = getSigString(transaction, translatedAddress);
      sigData = signTx(sigData, translatedAddress, name);
    } else {
      LOGGER.debug("Attempting to sign with userKey key");
      sigData = getSigString(transaction, address);
      sigData = signTx(sigData, address, name);
    }

    return applySignature(transaction, address, sigData);
  }

  private byte[][] signData(String data, String address, String name) {
    if (name == null) {
      String sig;
      try {
        LOGGER.debug("Asking eth node to sign " + data + " for 0x" + address);
        sig = ethereumWriteRpc.eth_sign("0x" + address, data);
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
          signingAddress = EthereumTools.getPublicAddress(signingAddress, false);
          LOGGER.debug("Appears to be signed by: " + signingAddress);
        } catch (Exception e) {
          LOGGER.debug("Couldn't recover public key from signature");
        }

        // Adjust for expected format.
        sigV[0] += 27;

        return new byte[][]{sigR, sigS, sigV};
      } catch (Exception e) {
        LOGGER.error(null, e);
        return new byte[0][0];
      }
    } else {
      int rounds = 100;

      String privateKey = "";
      if (address != null) {
        for (int i = 1; i <= rounds; i++) {
          String privateKeyCheck =
              EthereumTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), i);
          if (EthereumTools.getPublicAddress(privateKeyCheck).equalsIgnoreCase(address)) {
            privateKey = privateKeyCheck;
            break;
          }
        }
        if (privateKey.isEmpty()) {
          LOGGER.debug("Couldn't determine private key for address: " + address);
          return new byte[0][0];
        }
        LOGGER.debug("Found private key for address: " + address + " using user key.");
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
        // EIP-2
        BigInteger lowSlimit =
            new BigInteger("007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF5D576E7357A4501DDFE92F46681B20A0",
                16);
        BigInteger ourSvalue = new BigInteger(1, signedBytes[1]);
        while (ourSvalue.compareTo(lowSlimit) > 0) {
          signedBytes = Secp256k1.signTransaction(sigBytes, privateBytes);
          ourSvalue = new BigInteger(1, signedBytes[1]);
        }
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

  /**
   * Generate the data we need to sign the transaction offline.
   *
   * <p>If the address is a multi-sig contract then the first result will be the hash that needs to
   * be signed and inserted in the transaction.
   *
   * <p>The next result will be the original transaction along with the expected nonce for the
   * address provided.
   */
  public Iterable<Iterable<String>> getSigString(String transaction, String address)
      throws Exception {
    return getSigString(transaction, address, true);
  }

  public Iterable<Iterable<String>> getSigString(String transaction, String address,
      boolean withAnyMsig) throws Exception {
    // Turn the tx into a useful data structure.
    RawTransaction decodedTransaction =
        RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    String txRecipient = ByteUtilities.toHexString(decodedTransaction.getTo().getDecodedContents());

    LinkedList<Iterable<String>> sigStrings = new LinkedList<>();
    if (withAnyMsig) {
      try {
        if (reverseMsigContracts.containsKey(txRecipient.toLowerCase(Locale.US))) {
          ContractInformation contractInfo =
              msigContracts.get(reverseMsigContracts.get(txRecipient.toLowerCase(Locale.US)));
          MultiSigContractInterface contract =
              (MultiSigContractInterface) contractInfo.getContractVersion().newInstance();
          LOGGER.debug("Signing transaction for contract version: " + contract.getClass()
              .getCanonicalName());

          MultiSigContractParametersInterface contractParams = contract.getContractParameters();
          contractParams.decode(decodedTransaction.getData().getDecodedContents());
          String hashBytes = String.format("%64s", "0").replace(' ', '0');
          for (int j = 0; j < contractParams.getAddress().size(); j++) {
            String addressString =
                String.format("%40s", contractParams.getAddress().get(j)).replace(' ', '0');
            hashBytes = hashBytes + addressString.substring(addressString.length() - 40);
            hashBytes += String.format("%64s", ByteUtilities.toHexString(ByteUtilities
                .stripLeadingNullBytes(contractParams.getValue().get(j).toByteArray())))
                .replace(' ', '0') + String.format("%64s", ByteUtilities.toHexString(
                ByteUtilities.stripLeadingNullBytes(contractParams.getNonce().toByteArray())))
                .replace(' ', '0');
            LOGGER.debug("Hashing: " + hashBytes);
            hashBytes = EthereumTools.hashKeccak(hashBytes);
            LOGGER.debug("Result: " + hashBytes);
          }
          LOGGER.debug("Calculated contract sig-hash: " + hashBytes);
          LinkedList<String> msigString = new LinkedList<>();
          msigString.add(contract.getClass().getCanonicalName());
          msigString.add(hashBytes);
          sigStrings.add(msigString);
        }
      } catch (IllegalAccessException | InstantiationException e) {
        LOGGER.error("Error matching contract class", e);
        return new LinkedList<>();
      } catch (Exception e) {
        LOGGER.debug("Non-contract tx sent to contract address");
      }
    }

    String txCount =
        ethereumReadRpc.eth_getTransactionCount("0x" + address, DefaultBlock.LATEST.toString());
    LinkedList<String> txString = new LinkedList<>();
    txString.add(transaction);
    txString.add(txCount);
    sigStrings.add(txString);

    LOGGER.debug(sigStrings.toString());

    return sigStrings;
  }

  public Iterable<Iterable<String>> signWithPrivateKey(Iterable<Iterable<String>> data,
      String privateKey) {
    return signTx(data, null, privateKey);
  }

  /**
   * Signs transaction data based on results from getSigString.
   */
  public Iterable<Iterable<String>> signTx(Iterable<Iterable<String>> data, String address,
      String privateKey) {
    LOGGER.debug("Attempting to sign: " + address + data.toString());

    LinkedList<Iterable<String>> signedData = new LinkedList<>();
    LinkedList<Iterable<String>> listedData = new LinkedList<>();
    data.forEach(listedData::add);
    LinkedList<String> msigData = new LinkedList<>();
    LinkedList<String> txData = new LinkedList<>();
    // Check if there are two entries, if there are, the first one should be mSig data.
    int txDataLocation = 0;
    if (listedData.size() == 2) {
      txDataLocation++;
      listedData.get(0).forEach(msigData::add);
    }
    listedData.get(txDataLocation).forEach(txData::add);

    try {
      // Sign mSig if there is any
      if (msigData.size() > 0) {
        LOGGER.debug("Reading mSig data");
        String sigBytes = msigData.getLast();
        byte[][] sigData = signData(sigBytes, address, privateKey);
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
        String contractVersion = msigData.getFirst();
        MultiSigContractInterface contract =
            (MultiSigContractInterface) MultiSigContractInterface.class.getClassLoader()
                .loadClass(contractVersion).newInstance();

        MultiSigContractParametersInterface contractParms = contract.getContractParameters();
        contractParms.decode(rawTx.getData().getDecodedContents());

        Iterator<String> msigSig = signedData.getFirst().iterator();
        contractParms.getSigR().add(new BigInteger(1, ByteUtilities.toByteArray(msigSig.next())));
        contractParms.getSigS().add(new BigInteger(1, ByteUtilities.toByteArray(msigSig.next())));
        contractParms.getSigV().add(new BigInteger(1, ByteUtilities.toByteArray(msigSig.next())));
        rawTx.getData().setDecodedContents(contractParms.encode());
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
  public String sendTransaction(String transaction) throws Exception {
    transaction = ByteUtilities.toHexString(ByteUtilities.toByteArray(transaction));
    // If this is one of ours, re-sign the whole tx with the contract account.
    RawTransaction decodedTransaction =
        RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    String txRecipient = ByteUtilities.toHexString(decodedTransaction.getTo().getDecodedContents());

    try {
      if (reverseMsigContracts.containsKey(txRecipient.toLowerCase(Locale.US))) {
        ContractInformation contractInfo =
            msigContracts.get(reverseMsigContracts.get(txRecipient.toLowerCase(Locale.US)));
        MultiSigContractInterface contract =
            (MultiSigContractInterface) contractInfo.getContractVersion().newInstance();

        MultiSigContractParametersInterface contractParams = contract.getContractParameters();
        contractParams.decode(decodedTransaction.getData().getDecodedContents());

        if (contractParams.getFunction().equalsIgnoreCase(contract.getExecuteFunctionAddress())) {
          // This is a transfer request, re-sign it so that fees can be paid.
          String contractAddress = config.getContractAccount();
          String contractKey = config.getContractKey();

          if (!contractKey.isEmpty()) {
            contractAddress = EthereumTools.getPublicAddress(contractKey, true);
          } else {
            contractKey = null;
          }
          LOGGER.debug("Re-signing TX for fees: " + transaction);
          Iterable<Iterable<String>> sigData = getSigString(transaction, contractAddress, false);
          sigData = signTx(sigData, contractKey == null ? contractAddress : null, contractKey);
          transaction = applySignature(transaction, contractAddress, sigData);
        }
      }
    } catch (Exception e) {
      LOGGER.debug("Non-contract tx sent to contract address");
      LOGGER.trace(null, e);
    }

    try {
      LOGGER.debug("TX is signed by: " + ByteUtilities.toHexString(Secp256k1
          .recoverPublicKey(decodedTransaction.getSigR().getDecodedContents(),
              decodedTransaction.getSigS().getDecodedContents(),
              new byte[]{(byte) (decodedTransaction.getSigV().getDecodedContents()[0] - 27)},
              ByteUtilities.toByteArray(EthereumTools
                  .hashKeccak(ByteUtilities.toHexString(decodedTransaction.getSigBytes()))))));
    } catch (Exception e) {
      LOGGER.warn("Couldn't determine signer");
      LOGGER.trace(null, e);
    }
    LOGGER.debug("TX bytes: " + EthereumTools
        .hashKeccak(ByteUtilities.toHexString(decodedTransaction.getSigBytes())));
    if (transactionsEnabled) {
      return ethereumWriteRpc.eth_sendRawTransaction("0x" + transaction);
    } else {
      return "Transactions Temporarily Disabled";
    }
  }

  @Override
  public void setFeeRates(BigDecimal rate) {
    config.setGasPrice(rate.longValue());
  }

  @Override
  public Map<String, String> getConfiguration() {
    HashMap<String, String> configSummary = new HashMap<>();
    configSummary.put("Currency Symbol", config.getCurrencySymbol());
    configSummary.put("Geth Connection", config.getDaemonConnectionString());
    configSummary.put("Minimum Signatures", ((Integer) config.getMinSignatures()).toString());
    configSummary.put("Minimum Confirmations", ((Integer) config.getMinConfirmations()).toString());
    configSummary
        .put("Maximum Transaction Value", config.getMaxAmountPerTransaction().toPlainString());
    configSummary
        .put("Maximum Transaction Value Per Hour", config.getMaxAmountPerHour().toPlainString());
    configSummary
        .put("Maximum Transaction Value Per Day", config.getMaxAmountPerDay().toPlainString());
    return configSummary;
  }

  private boolean transactionsEnabled = true;

  @Override
  public void enableTransactions() {
    transactionsEnabled = true;
  }

  @Override
  public void disableTransactions() {
    transactionsEnabled = false;
  }

  @Override
  public boolean transactionsEnabled() {
    return transactionsEnabled;
  }

  @Override
  public long getBlockchainHeight() throws Exception {
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumReadRpc.eth_blockNumber()));
    return latestBlockNumber.longValue();
  }

  @Override
  public long getLastBlockTime() throws Exception {
    Block block = ethereumReadRpc.eth_getBlockByNumber(DefaultBlock.LATEST.getValue(), true);
    BigInteger dateConverter = new BigInteger(1, ByteUtilities.toByteArray(block.getTimestamp()));
    return dateConverter.longValue();
  }

  private class TxDateComparator implements Comparator<TransactionDetails> {
    @Override
    public int compare(TransactionDetails o1, TransactionDetails o2) {
      return o1.getTxDate().compareTo(o2.getTxDate());
    }
  }


  private static HashMap<String, String> txFilterIds = new HashMap<>();
  private static HashMap<String, LinkedList<Map<String, Object>>> cachedTxFilterResults =
      new HashMap<>();

  @Override
  public TransactionDetails[] getTransactions(String address, int numberToReturn, int skipNumber)
      throws Exception {
    // Get latest block
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumReadRpc.eth_blockNumber()));

    address = "0x" + ByteUtilities.toHexString(ByteUtilities.toByteArray(address));

    LinkedList<TransactionDetails> txDetails = new LinkedList<>();
    Map<String, Object> filterParams = new HashMap<>();
    filterParams.put("fromBlock", "0x0");
    filterParams.put("toBlock", "latest");
    filterParams.put("address", address);
    LOGGER.debug("Filter: " + Json.stringifyObject(Map.class, filterParams));
    String txFilter = "";
    if (txFilterIds.containsKey(Json.stringifyObject(Map.class, filterParams))) {
      txFilter = txFilterIds.get(Json.stringifyObject(Map.class, filterParams));
    } else {
      txFilter = ethereumReadRpc.eth_newFilter(filterParams);
      txFilterIds.put(Json.stringifyObject(Map.class, filterParams), txFilter);
    }
    Map<String, Object>[] filterResults = ethereumReadRpc.eth_getFilterChanges(txFilter);
    if (cachedTxFilterResults.containsKey(txFilter)) {
      LinkedList<Map<String, Object>> cachedResults = cachedTxFilterResults.get(txFilter);
      cachedResults.addAll(Arrays.asList(filterResults));
      cachedTxFilterResults.put(txFilter, cachedResults);
    } else {
      cachedTxFilterResults.put(txFilter, new LinkedList<>(Arrays.asList(filterResults)));
    }
    filterResults = (Map<String, Object>[]) cachedTxFilterResults.get(txFilter).toArray();
    for (Map<String, Object> result : filterResults) {
      LOGGER.error(result.toString());
      TransactionDetails txDetail = new TransactionDetails();
      txDetail.setTxHash((String) result.get("transactionHash"));
      try {
        Block block =
            ethereumReadRpc.eth_getBlockByNumber((String) result.get("blockNumber"), true);
        BigInteger dateConverter =
            new BigInteger(1, ByteUtilities.toByteArray(block.getTimestamp()));
        dateConverter = dateConverter.multiply(BigInteger.valueOf(1000));
        txDetail.setTxDate(new Date(dateConverter.longValue()));

        BigInteger txBlockNumber =
            new BigInteger(1, ByteUtilities.toByteArray((String) result.get("blockNumber")));
        txDetail.setConfirmed(
            config.getMinConfirmations() <= latestBlockNumber.subtract(txBlockNumber).intValue());
        txDetail.setConfirmations(latestBlockNumber.subtract(txBlockNumber).intValue());
        txDetail.setMinConfirmations(config.getMinConfirmations());
      } catch (Exception e) {
        // Pending TX
        LOGGER.debug("Pending Tx Found", e);
      }

      String to = ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(
          ByteUtilities.readBytes(ByteUtilities.toByteArray((String) result.get("data")), 0, 32)));
      txDetail.setToAddress(new String[]{to});

      String from = ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(
          ByteUtilities.readBytes(ByteUtilities.toByteArray((String) result.get("data")), 32, 32)));
      txDetail.setFromAddress(new String[]{from});

      String amount = ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(
          ByteUtilities.readBytes(ByteUtilities.toByteArray((String) result.get("data")), 64, 32)));
      txDetail.setAmount(new BigDecimal(new BigInteger(1, ByteUtilities.toByteArray(amount)))
          .divide(BigDecimal.valueOf(config.getWeiMultiplier()), BigDecimal.ROUND_HALF_UP));
      txDetails.add(txDetail);
    }

    LOGGER.debug("Size: " + txDetails.size());

    LOGGER.debug("SkipNumber: " + skipNumber);
    LOGGER.debug("NumberToReturn: " + numberToReturn);
    Collections.sort(txDetails, new TxDateComparator());
    for (int i = 0; i < skipNumber; i++) {
      txDetails.removeLast();
    }
    LOGGER.debug("Size: " + txDetails.size());
    while (txDetails.size() > numberToReturn) {
      txDetails.removeFirst();
    }
    LOGGER.debug("Size: " + txDetails.size());
    return txDetails.toArray(new TransactionDetails[txDetails.size()]);
  }

  @Override
  public TransactionDetails getTransaction(String transactionId) throws Exception {
    Map txData = ethereumReadRpc.eth_getTransactionByHash(transactionId);

    Block txBlock =
        ethereumReadRpc.eth_getBlockByNumber(txData.get("blockNumber").toString(), true);
    TransactionDetails txDetail = new TransactionDetails();
    txDetail.setTxHash(txData.get("hash").toString());
    txDetail.setTxDate(new Date(
        new BigInteger(1, ByteUtilities.toByteArray(txBlock.getTimestamp())).longValue() * 1000L));
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumReadRpc.eth_blockNumber()));
    BigInteger txBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(txData.get("blockNumber").toString()));
    txDetail.setConfirmed(
        config.getMinConfirmations() <= latestBlockNumber.subtract(txBlockNumber).intValue());
    txDetail.setConfirmations(latestBlockNumber.subtract(txBlockNumber).intValue());
    txDetail.setMinConfirmations(config.getMinConfirmations());

    txDetail.setAmount(
        new BigDecimal(new BigInteger(1, ByteUtilities.toByteArray(txData.get("value").toString())))
            .divide(BigDecimal.valueOf(config.getWeiMultiplier())));
    txDetail.setToAddress(new String[]{txData.get("to").toString()});
    txDetail.setFromAddress(new String[]{txData.get("from").toString()});

    try {
      if (reverseMsigContracts.containsKey(txDetail.getToAddress()[0].toLowerCase(Locale.US))) {
        ContractInformation contractInfo = msigContracts
            .get(reverseMsigContracts.get(txDetail.getToAddress()[0].toLowerCase(Locale.US)));
        MultiSigContractInterface contract =
            (MultiSigContractInterface) contractInfo.getContractVersion().newInstance();

        byte[] inputData = ByteUtilities.toByteArray(txData.get("input").toString());
        MultiSigContractParametersInterface multiSig = contract.getContractParameters();
        multiSig.decode(inputData);

        LOGGER.debug("TXDetail: " + txDetail.toString());
        if (multiSig.getFunction().equalsIgnoreCase(contract.getExecuteFunctionAddress())) {
          txDetail.setFromAddress(txDetail.getToAddress());
          txDetail.setToAddress(new String[0]);
          txDetail.setAmount(BigDecimal.ZERO);
          for (int j = 0; j < multiSig.getAddress().size(); j++) {
            LinkedList<String> recipients = new LinkedList<>();
            recipients.addAll(Arrays.asList(txDetail.getToAddress()));
            recipients.add(multiSig.getAddress().get(j));
            txDetail.setToAddress(recipients.toArray(new String[recipients.size()]));

            txDetail.setAmount(txDetail.getAmount().add(
                BigDecimal.valueOf(multiSig.getValue().get(j).longValue())
                    .divide(BigDecimal.valueOf(config.getWeiMultiplier()))));
          }

          LOGGER.debug("Updated TXDetail: " + txDetail.toString());
        }
      }
    } catch (Exception e) {
      LOGGER.debug("Not a contract tx");
    }
    return txDetail;
  }

  @Override
  public TransactionDetails decodeRawTransaction(String transaction) {
    RawTransaction tx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    TransactionDetails txDetail = new TransactionDetails();
    txDetail.setAmount(new BigDecimal(new BigInteger(1, tx.getValue().getDecodedContents())));
    txDetail.setAmount(txDetail.getAmount().divide(BigDecimal.valueOf(config.getWeiMultiplier())));

    try {
      String senderKey = ByteUtilities.toHexString(Secp256k1
          .recoverPublicKey(tx.getSigR().getDecodedContents(), tx.getSigS().getDecodedContents(),
              tx.getSigV().getDecodedContents(), tx.getSigBytes()));
      String sender = EthereumTools.getPublicAddress(senderKey, false);
      txDetail.setFromAddress(new String[]{sender});
    } catch (ArrayIndexOutOfBoundsException e) {
      LOGGER.debug("Unsigned transaction, can't determine sender");
      txDetail.setFromAddress(new String[]{});
    }

    txDetail.setToAddress(new String[]{ByteUtilities.toHexString(tx.getTo().getDecodedContents())});

    // Check if the recipient/data show that we're talking to a contract.
    try {
      if (reverseMsigContracts.containsKey(txDetail.getToAddress()[0].toLowerCase(Locale.US))) {
        ContractInformation contractInfo = msigContracts
            .get(reverseMsigContracts.get(txDetail.getToAddress()[0].toLowerCase(Locale.US)));
        MultiSigContractInterface contract =
            (MultiSigContractInterface) contractInfo.getContractVersion().newInstance();

        byte[] inputData = tx.getData().getDecodedContents();
        MultiSigContractParametersInterface multiSig = contract.getContractParameters();
        multiSig.decode(inputData);

        LOGGER.debug("TXDetail: " + txDetail.toString());
        if (multiSig.getFunction().equalsIgnoreCase(contract.getExecuteFunctionAddress())) {
          txDetail.setFromAddress(txDetail.getToAddress());
          txDetail.setToAddress(new String[0]);
          txDetail.setAmount(BigDecimal.ZERO);
          for (int j = 0; j < multiSig.getAddress().size(); j++) {
            LinkedList<String> recipients = new LinkedList<>();
            recipients.addAll(Arrays.asList(txDetail.getToAddress()));
            recipients.add(multiSig.getAddress().get(j));
            txDetail.setToAddress(recipients.toArray(new String[recipients.size()]));

            txDetail.setAmount(txDetail.getAmount().add(
                BigDecimal.valueOf(multiSig.getValue().get(j).longValue())
                    .divide(BigDecimal.valueOf(config.getWeiMultiplier()))));
          }

          LOGGER.debug("Updated TXDetail: " + txDetail.toString());
        }
      }
    } catch (ArrayIndexOutOfBoundsException | InstantiationException | IllegalAccessException e) {
      LOGGER.debug("Unable to decode tx data");
    }

    return txDetail;
  }

  @Override
  public ServerStatus getWalletStatus() {
    try {
      ethereumReadRpc.eth_blockNumber();
      return ServerStatus.CONNECTED;
    } catch (Exception e) {
      return ServerStatus.DISCONNECTED;
    }
  }
}
