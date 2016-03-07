package io.emax.cosigner.ethereum;

import io.emax.cosigner.api.core.ServerStatus;
import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.api.validation.Validatable;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.crypto.Secp256k1;
import io.emax.cosigner.ethereum.common.EthereumTools;
import io.emax.cosigner.ethereum.common.RlpItem;
import io.emax.cosigner.ethereum.common.RlpList;
import io.emax.cosigner.ethereum.gethrpc.Block;
import io.emax.cosigner.ethereum.gethrpc.CallData;
import io.emax.cosigner.ethereum.gethrpc.DefaultBlock;
import io.emax.cosigner.ethereum.gethrpc.EthereumRpc;
import io.emax.cosigner.ethereum.gethrpc.RawTransaction;
import io.emax.cosigner.ethereum.gethrpc.multisig.ContractInformation;
import io.emax.cosigner.ethereum.gethrpc.multisig.MultiSigContract;
import io.emax.cosigner.ethereum.gethrpc.multisig.MultiSigContractInterface;
import io.emax.cosigner.ethereum.gethrpc.multisig.MultiSigContractParametersInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Subscription;

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
import java.util.concurrent.TimeUnit;

public class EthereumWallet implements Wallet, Validatable {
  private static final Logger LOGGER = LoggerFactory.getLogger(EthereumWallet.class);

  // RPC and configuration
  private static final EthereumRpc ethereumRpc = EthereumResource.getResource().getGethRpc();
  private static final EthereumConfiguration config = new EthereumConfiguration();

  // Address generation data
  private static final HashMap<String, Integer> addressRounds = new HashMap<>();

  // Multi-sig data
  private static final HashMap<String, ContractInformation> msigContracts = new HashMap<>();
  private static final HashMap<String, String> reverseMsigContracts = new HashMap<>();
  @SuppressWarnings("unused")
  private static Subscription multiSigSubscription =
      Observable.interval(1, TimeUnit.MINUTES).onErrorReturn(null)
          .subscribe(tick -> syncMultiSigAddresses());

  // Transaction history data
  private static final HashMap<String, HashSet<TransactionDetails>> txHistory = new HashMap<>();
  @SuppressWarnings("unused")
  private static Subscription txHistorySubscription =
      Observable.interval(1, TimeUnit.MINUTES).onErrorReturn(null)
          .subscribe(tick -> scanTransactions());

  /**
   * Ethereum wallet.
   *
   * <p>Provides wallet access to the Ethereum network via a geth node.
   */
  public EthereumWallet() {
    try {
      syncMultiSigAddresses();
    } catch (Exception e) {
      LOGGER.debug(null, e);
    }
  }

  private static volatile boolean synching = false;

  private static void syncMultiSigAddresses() {
    if (synching) {
      return;
    }
    try {
      synching = true;
      LOGGER.debug("Synchronizing contract accounts with network...");
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
        String contractCode = ethereumRpc
            .eth_getCode("0x" + contract.toLowerCase(Locale.US), DefaultBlock.LATEST.toString());

        contractCode = contractCode.substring(2);
        LOGGER.debug("Contract code: " + contractCode);
        Class<?> contractType = MultiSigContract.class;
        while (MultiSigContractInterface.class.isAssignableFrom(contractType)) {
          MultiSigContractInterface contractParams =
              (MultiSigContractInterface) contractType.newInstance();
          if (contractParams.getContractPayload().equalsIgnoreCase(contractCode)) {
            // We found an existing contract
            LOGGER.debug("Found existing contract version: " + contractType.getCanonicalName());
            CallData callData = new CallData();
            callData.setTo("0x" + contract);
            callData.setData("0x" + contractParams.getGetOwnersFunctionAddress());
            callData.setGas("100000"); // Doesn't matter, just can't be nil
            callData.setGasPrice("100000"); // Doesn't matter, just can't be nil
            String response = ethereumRpc.eth_call(callData, DefaultBlock.LATEST.toString());

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

              // Skip it if we already know about it
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
          }

          contractType = contractType.getSuperclass();
        }
      }
    } catch (InstantiationException | IllegalAccessException e) {
      LOGGER.debug(null, e);
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
    // Generate the next private key
    LOGGER.debug("Creating a new normal address...");
    int rounds = 1 + skipNumber;
    String privateKey =
        EthereumTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);

    // Convert to an Ethereum address
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
      // Generate rounds
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
  public String getMultiSigAddress(Iterable<String> addresses, String name) {
    // Look for existing msig account for this address.
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
    // Create the TX data structure
    RawTransaction tx = new RawTransaction();
    tx.getGasPrice().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
    tx.getGasLimit().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));

    // Setup parameters for contract
    // Parameters for constructor are appended after the contract code
    // Each value is a 32-byte hex entry, one after the next with no delimiters
    // Addresses[] - because it's an array we provide a pointer relative to the input data start,
    // showing where you can find the data
    final String contractInit = new MultiSigContract().getInitData();
    final String accountOffset = String.format("%64s", "40").replace(' ', '0');
    // Required sigs
    String requiredSigs =
        ByteUtilities.toHexString(BigInteger.valueOf(config.getMinSignatures()).toByteArray());
    requiredSigs = String.format("%64s", requiredSigs).replace(' ', '0');
    // Address[] - first entry in an array parameter is how many elements there are

    // Build the array
    for (int i = 0; i < config.getMultiSigAddresses().length; i++) {
      if (config.getMultiSigAddresses()[i].isEmpty()) {
        continue;
      }
      addressesUsed.add(String.format("%64s", config.getMultiSigAddresses()[i]).replace(' ', '0'));
    }
    String numberOfAddresses =
        ByteUtilities.toHexString(BigInteger.valueOf(addressesUsed.size()).toByteArray());
    numberOfAddresses = String.format("%64s", numberOfAddresses).replace(' ', '0');

    // Contract code is the init code which copies the payload and constructor parameters, then runs
    // the constructor
    // Followed by the payload, i.e. contract code that gets installed
    // Followed by the constructor params.
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
    LOGGER.debug("Attempting to create contract with account: " + config.getContractAccount());
    String signedTx = signTransaction(rawTx, config.getContractAccount());

    // Signature failed, we got the same thing back
    if (signedTx.equalsIgnoreCase(rawTx)) {
      return "";
    }

    // According to yellow paper address should be RLP(Sender, nonce)
    // Nonce is only filled when we sign so grab the new value now
    // We use this to predict the address, instead of waiting for a receipt.
    tx = RawTransaction.parseBytes(ByteUtilities.toByteArray(signedTx));
    RlpList contractAddress = new RlpList();
    RlpItem contractCreator = new RlpItem(ByteUtilities.toByteArray(config.getContractAccount()));
    contractAddress.add(contractCreator);
    contractAddress.add(tx.getNonce());

    // Figure out the contract address and store it in lookup tables for future use
    String contract = EthereumTools.hashKeccak(ByteUtilities.toHexString(contractAddress.encode()))
        .substring(96 / 4, 256 / 4);

    LOGGER.debug("Expecting new contract address of " + contract + " with tx: " + RawTransaction
        .parseBytes(ByteUtilities.toByteArray(signedTx)));

    // Figure out if we already created this, if so, skip it...
    if (reverseMsigContracts.containsKey(contract.toLowerCase(Locale.US))) {
      return "";
    }

    // Make sure it's sent first.
    sendTransaction(signedTx);

    // Otherwise, register it
    msigContracts.put(userAddress, new ContractInformation(contract.toLowerCase(Locale.US),
        new MultiSigContract().getContractPayload(), MultiSigContract.class));
    reverseMsigContracts.put(contract.toLowerCase(Locale.US), userAddress);

    return contract;
  }

  @Override
  public String getBalance(String address) {
    // Get latest block
    BigInteger latestBlockNumber =
        new BigInteger("00" + ethereumRpc.eth_blockNumber().substring(2), 16);
    BigInteger confirmedBlockNumber =
        latestBlockNumber.subtract(BigInteger.valueOf(config.getMinConfirmations()));

    // Get balance at latest & latest - (min conf)
    BigInteger latestBalance = new BigInteger(
        "00" + ethereumRpc.eth_getBalance(address, "0x" + latestBlockNumber.toString(16))
            .substring(2), 16);
    BigInteger confirmedBalance = new BigInteger(
        "00" + ethereumRpc.eth_getBalance(address, "0x" + confirmedBlockNumber.toString(16))
            .substring(2), 16);

    // convert to Ether and return the lower of the two
    confirmedBalance = confirmedBalance.min(latestBalance);
    BigDecimal etherBalance =
        new BigDecimal(confirmedBalance).divide(BigDecimal.valueOf(config.getWeiMultiplier()));
    return etherBalance.toPlainString();
  }

  @Override
  public String createTransaction(Iterable<String> fromAddress, Iterable<Recipient> toAddress) {

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

    // Create the transaction structure and serialize it
    RawTransaction tx = new RawTransaction();
    tx.getGasPrice().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
    tx.getGasLimit().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getSimpleTxGas()).toByteArray()));
    tx.getTo().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(new BigInteger(recipient.getRecipientAddress(), 16).toByteArray()));
    tx.getValue().setDecodedContents(
        ByteUtilities.stripLeadingNullBytes(amountWei.toBigInteger().toByteArray()));

    // If we're sending this from one of our msig accounts
    // We need to restructure things a little
    // TX info like to/from are moved into data, and the to is pointed to the contract.
    try {
      if (isMsigSender) {
        // move things around to match the contract
        // Increase gas to contract amount
        tx.getGasLimit().setDecodedContents(ByteUtilities
            .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));

        // Clear the value, it's part of the data
        tx.getValue().setDecodedContents(new byte[]{});

        // Figure out which contract we're using.
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

        // For this particular contract the previous nonce is stored at 0x0a.
        // It will only accept nonce's one greater than this.
        String txCount = ethereumRpc
            .eth_getStorageAt("0x" + senderAddress.toLowerCase(Locale.US), "0x1",
                DefaultBlock.LATEST.toString());
        BigInteger nonce =
            new BigInteger(1, ByteUtilities.toByteArray(txCount)).add(BigInteger.ONE);
        contractParms.setNonce(nonce);

        tx.getData().setDecodedContents(contractParms.encode());

        // Change the recipient
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

    // We can only give information on the contracts we control.
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
        callData.setData("0x" + contract.getGetOwnersFunctionAddress());
        callData.setGas("100000"); // Doesn't matter, just can't be nil
        callData.setGasPrice("100000"); // Doesn't matter, just can't be nil
        String response = ethereumRpc.eth_call(callData, DefaultBlock.LATEST.toString());

        LinkedList<String> addresses = new LinkedList<>();
        byte[] callBytes = ByteUtilities.toByteArray(response);
        int bufferPointer = 32; // skip first value, we know it just points to the next one.
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
        LOGGER.debug("Something went wrong with decoding the contract.", e);
        return new LinkedList<String>();
      }
    } else {
      return new LinkedList<String>();
    }
  }

  @Override
  public String signTransaction(String transaction, String address) {
    LOGGER.debug("Attempting to sign for address: " + address);
    return signTransaction(transaction, address, null);
  }

  @Override
  public String signTransaction(String transaction, String address, String name) {
    // Verify that the account is capable of sending this.
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
      for (int i = 0; i < config.getMultiSigAddresses().length; i++) {
        if (config.getMultiSigAddresses()[i].isEmpty()) {
          continue;
        }
        sigData = getSigString(transaction, config.getMultiSigAddresses()[i]);
        sigData = signTx(sigData, config.getMultiSigAddresses()[i], null);
        transaction = applySignature(transaction, address, sigData);
      }
    } else if (name == null) {
      sigData = getSigString(transaction, address);
      sigData = signTx(sigData, address, null);
      transaction = applySignature(transaction, address, sigData);
    } else {
      String translatedAddress = reverseMsigContracts.get(address.toLowerCase(Locale.US));
      sigData = getSigString(transaction, translatedAddress);
      sigData = signTx(sigData, translatedAddress, name);
    }

    return applySignature(transaction, address, sigData);
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
      int rounds = 1;
      if (address != null) {
        if (addressRounds.containsKey(name)) {
          rounds = addressRounds.get(name);
        } else {
          // Generate rounds
          createAddress(name);
          rounds = addressRounds.get(name);
        }
      }

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
  public Iterable<Iterable<String>> getSigString(String transaction, String address) {
    return getSigString(transaction, address, true);
  }

  public Iterable<Iterable<String>> getSigString(String transaction, String address,
      boolean withAnyMsig) {
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
        LOGGER.debug("Non-contract tx sent to contract address", e);
      }
    }

    String txCount =
        ethereumRpc.eth_getTransactionCount("0x" + address, DefaultBlock.LATEST.toString());
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
  public String sendTransaction(String transaction) {
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
          LOGGER.debug("Re-signing TX for fees: " + transaction);
          Iterable<Iterable<String>> sigData =
              getSigString(transaction, config.getContractAccount(), false);
          sigData = signTx(sigData, config.getContractAccount(), null);
          transaction = applySignature(transaction, config.getContractAccount(), sigData);
        }
      }
    } catch (Exception e) {
      LOGGER.debug("Non-contract tx sent to contract address", e);
    }

    try {
      LOGGER.debug("TX is signed by: " + ByteUtilities.toHexString(Secp256k1
          .recoverPublicKey(decodedTransaction.getSigR().getDecodedContents(),
              decodedTransaction.getSigS().getDecodedContents(),
              new byte[]{(byte) (decodedTransaction.getSigV().getDecodedContents()[0] - 27)},
              ByteUtilities.toByteArray(EthereumTools
                  .hashKeccak(ByteUtilities.toHexString(decodedTransaction.getSigBytes()))))));
    } catch (Exception e) {
      LOGGER.error("Couldn't determine signer", e);
    }
    LOGGER.debug("TX bytes: " + EthereumTools
        .hashKeccak(ByteUtilities.toHexString(decodedTransaction.getSigBytes())));
    return ethereumRpc.eth_sendRawTransaction(transaction);
  }

  private static void scanTransactions() {
    // Scan every block, look for origin and receiver.
    // Get latest block
    BigInteger latestBlockNumber =
        new BigInteger(1, ByteUtilities.toByteArray(ethereumRpc.eth_blockNumber()));

    for (long i = 0; i < latestBlockNumber.longValue(); i++) {
      String blockNumber = "0x" + BigInteger.valueOf(i).toString(16);
      Block block = ethereumRpc.eth_getBlockByNumber(blockNumber, true);

      if (block.getTransactions().length == 0) {
        continue;
      }

      Arrays.asList(block.getTransactions()).forEach(tx -> {
        TransactionDetails txDetail = new TransactionDetails();
        txDetail.setTxDate(block.getTimestamp());

        txDetail.setTxHash(ByteUtilities.toHexString(ByteUtilities.toByteArray(tx.getHash())));
        txDetail.setFromAddress(
            new String[]{ByteUtilities.toHexString(ByteUtilities.toByteArray(tx.getFrom()))});
        txDetail.setToAddress(
            new String[]{ByteUtilities.toHexString(ByteUtilities.toByteArray(tx.getTo()))});
        BigDecimal amount =
            new BigDecimal(new BigInteger(1, ByteUtilities.toByteArray(tx.getValue())));
        amount = amount.divide(BigDecimal.valueOf(config.getWeiMultiplier()));
        txDetail.setAmount(amount);

        // For each receiver that is an mSig account, parse the data, check if it's sending data to
        // another account.
        try {
          if (reverseMsigContracts.containsKey(txDetail.getToAddress()[0].toLowerCase(Locale.US))) {
            ContractInformation contractInfo = msigContracts
                .get(reverseMsigContracts.get(txDetail.getToAddress()[0].toLowerCase(Locale.US)));
            MultiSigContractInterface contract =
                (MultiSigContractInterface) contractInfo.getContractVersion().newInstance();
            LOGGER.debug("Found transaction for contract version: " + contract.getClass()
                .getCanonicalName());

            byte[] inputData = ByteUtilities.toByteArray(tx.getInput());
            MultiSigContractParametersInterface multiSig = contract.getContractParameters();
            multiSig.decode(inputData);

            if (multiSig.getFunction().equalsIgnoreCase(contract.getExecuteFunctionAddress())) {
              for (int j = 0; j < multiSig.getAddress().size(); j++) {
                TransactionDetails msigTx = new TransactionDetails();
                msigTx.setFromAddress(txDetail.getToAddress());
                msigTx.setToAddress(new String[]{multiSig.getAddress().get(j)});
                msigTx.setAmount(BigDecimal.valueOf(multiSig.getValue().get(j).longValue())
                    .divide(BigDecimal.valueOf(config.getWeiMultiplier())));
                msigTx.setTxHash(txDetail.getTxHash());

                if (!txHistory.containsKey(msigTx.getToAddress()[0])) {
                  txHistory.put(msigTx.getToAddress()[0], new HashSet<>());
                }
                if (!txHistory.containsKey(msigTx.getFromAddress()[0])) {
                  txHistory.put(msigTx.getFromAddress()[0], new HashSet<>());
                }

                if (reverseMsigContracts.containsKey(msigTx.getFromAddress()[0])) {
                  txHistory.get(msigTx.getFromAddress()[0]).add(msigTx);
                }
                if (reverseMsigContracts.containsKey(msigTx.getToAddress()[0])) {
                  txHistory.get(msigTx.getToAddress()[0]).add(msigTx);
                }
              }
            }
          }
        } catch (Exception e) {
          LOGGER.debug("Unable to decode tx data", e);
        }

        if (!txHistory.containsKey(txDetail.getToAddress()[0])) {
          txHistory.put(txDetail.getToAddress()[0], new HashSet<>());
        }
        if (!txHistory.containsKey(txDetail.getFromAddress()[0])) {
          txHistory.put(txDetail.getFromAddress()[0], new HashSet<>());
        }

        if (reverseMsigContracts.containsKey(txDetail.getFromAddress()[0])) {
          txHistory.get(txDetail.getFromAddress()[0]).add(txDetail);
        }
        if (reverseMsigContracts.containsKey(txDetail.getToAddress()[0])) {
          txHistory.get(txDetail.getToAddress()[0]).add(txDetail);
        }
      });
    }
  }

  @Override
  public TransactionDetails[] getTransactions(String address, int numberToReturn, int skipNumber) {
    LinkedList<TransactionDetails> txDetails = new LinkedList<>();
    if (txHistory.containsKey(address)) {
      txHistory.get(address).forEach(txDetails::add);
    }
    return txDetails.toArray(new TransactionDetails[txDetails.size()]);
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
      LOGGER.debug("Unsigned transaction, can't determine sender", e);
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
      LOGGER.debug("Unable to decode tx data", e);
    }

    return txDetail;
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
}
