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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EthereumWallet implements Wallet, Validatable {
  private static final Logger LOGGER = LoggerFactory.getLogger(EthereumWallet.class);

  // RPC and configuration
  private static EthereumRpc ethereumRpc = EthereumResource.getResource().getGethRpc();
  private static EthereumConfiguration config = new EthereumConfiguration();

  // Address generation data
  private static HashMap<String, Integer> addressRounds = new HashMap<>();

  // Multi-sig data
  private static HashMap<String, ContractInformation> msigContracts = new HashMap<>();
  private static HashMap<String, String> reverseMsigContracts = new HashMap<>();
  @SuppressWarnings("unused")
  private static Subscription multiSigSubscription = Observable.interval(1, TimeUnit.MINUTES)
      .onErrorReturn(null).subscribe(tick -> syncMultiSigAddresses());

  // Transaction history data
  private static HashMap<String, HashSet<TransactionDetails>> txHistory = new HashMap<>();
  @SuppressWarnings("unused")
  private static Subscription txHistorySubscription = Observable.interval(1, TimeUnit.MINUTES)
      .onErrorReturn(null).subscribe(tick -> scanTransactions());

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

  private static synchronized void syncMultiSigAddresses() {
    try {
      String contractPayload = "0x" + new MultiSigContract().getContractPayload();
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
            EthereumTools.hashSha3(ByteUtilities.toHexString(contractAddress.encode()))
                .substring(96 / 4, 256 / 4);
        String contractCode = ethereumRpc.eth_getCode("0x" + contract.toLowerCase(Locale.US),
            DefaultBlock.LATEST.toString());

        Class<?> contractType = MultiSigContract.class;
        while (MultiSigContractInterface.class.isAssignableFrom(contractType)) {
          MultiSigContractInterface contractParams =
              (MultiSigContractInterface) contractType.newInstance();
          if (contractParams.getContractPayload().equalsIgnoreCase(contractPayload)) {
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

              msigContracts.put(userAddress.toLowerCase(Locale.US), new ContractInformation(
                  contract.toLowerCase(Locale.US), contractCode, contractType));
              reverseMsigContracts.put(contract.toLowerCase(Locale.US),
                  userAddress.toLowerCase(Locale.US));
            }
          }

          contractType = contractType.getSuperclass();
        }
      }
    } catch (InstantiationException | IllegalAccessException e) {
      LOGGER.debug(null, e);
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
  public Iterable<String> getAddresses(String name) {
    int maxRounds = 1;
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
    String userAddress = addresses.iterator().next().toLowerCase(Locale.US);
    if (msigContracts.containsKey(userAddress.toLowerCase(Locale.US))) {
      LOGGER.debug("Found existing address: "
          + msigContracts.get(userAddress).getContractAddress().toLowerCase(Locale.US));
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
    // Each value is a 64-byte hex entry, one after the next with no delimiters
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
    String[] addressesUsed = new String[config.getMultiSigAddresses().length + 1];
    addressesUsed[0] = String.format("%64s", userAddress).replace(' ', '0');
    int addressesSkipped = 0;
    for (int i = 0; i < config.getMultiSigAddresses().length; i++) {
      if (config.getMultiSigAddresses()[i].isEmpty()) {
        addressesSkipped++;
        continue;
      }
      addressesUsed[i + 1] =
          String.format("%64s", config.getMultiSigAddresses()[i]).replace(' ', '0');
    }
    String numberOfAddresses = ByteUtilities.toHexString(BigInteger
        .valueOf(config.getMultiSigAddresses().length + 1 - addressesSkipped).toByteArray());
    numberOfAddresses = String.format("%64s", numberOfAddresses).replace(' ', '0');

    // Contract code is the init code which copies the payload and constructor parameters, then runs
    // the constructor
    // Followed by the payload, i.e. contract code that gets installed
    // Followed by the constructor params.
    StringBuilder contractCode = new StringBuilder();
    contractCode.append(contractInit + accountOffset + requiredSigs + numberOfAddresses);
    for (String addr : addressesUsed) {
      contractCode.append(addr);
    }
    tx.getData().setDecodedContents(ByteUtilities.toByteArray(contractCode.toString()));

    // Sign it with our contract creator, creator needs funds to pay for the creation
    String rawTx = ByteUtilities.toHexString(tx.encode());
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
    String contract = EthereumTools.hashSha3(ByteUtilities.toHexString(contractAddress.encode()))
        .substring(96 / 4, 256 / 4);

    LOGGER.debug("Expecting new contract address of " + contract + " with tx: "
        + RawTransaction.parseBytes(ByteUtilities.toByteArray(signedTx)));

    // Figure out if we already created this, if so, skip it...
    if (reverseMsigContracts.containsKey(contract.toLowerCase(Locale.US))) {
      return "";
    }

    // Otherwise, register it
    msigContracts.put(userAddress, new ContractInformation(contract.toLowerCase(Locale.US),
        new MultiSigContract().getContractPayload(), MultiSigContract.class));
    reverseMsigContracts.put(contract.toLowerCase(Locale.US), userAddress);

    sendTransaction(signedTx);
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
    BigInteger latestBalance = new BigInteger("00"
        + ethereumRpc.eth_getBalance(address, "0x" + latestBlockNumber.toString(16)).substring(2),
        16);
    BigInteger confirmedBalance = new BigInteger("00" + ethereumRpc
        .eth_getBalance(address, "0x" + confirmedBlockNumber.toString(16)).substring(2), 16);

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
        tx.getValue().setDecodedContents(new byte[] {});

        // Figure out which contract we're using.
        MultiSigContractInterface contract = (MultiSigContractInterface) msigContracts
            .get(possibleSenders.getFirst()).getContractVersion().newInstance();
        LOGGER.debug(
            "Creating transaction for contract version: " + contract.getClass().getCanonicalName());
        MultiSigContractParametersInterface contractParms = contract.getContractParameters();
        contractParms.setFunction(contract.getExecuteFunctionAddress());
        toAddress.iterator().forEachRemaining(rcpt -> {
          contractParms.getAddress().add(rcpt.getRecipientAddress());
          contractParms.getValue().add(rcpt.getAmount()
              .multiply(BigDecimal.valueOf(config.getWeiMultiplier())).toBigInteger());
        });

        // For this particular contract the previous nonce is stored at 0x0a.
        // It will only accept nonce's one greater than this.
        String txCount = ethereumRpc.eth_getStorageAt("0x" + senderAddress.toLowerCase(Locale.US),
            "0x1", DefaultBlock.LATEST.toString());
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
  public String signTransaction(String transaction, String address) {
    return signTransaction(transaction, address, null);
  }

  @Override
  public String signTransaction(String transaction, String address, String name) {
    try {
      // Validate the transaction data
      RawTransaction decodedTransaction =
          RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));

      // We've been asked to sign something for the multi-sig contract without a user-key
      // Going on the assumption that the local geth wallet only has one key
      if (name == null && reverseMsigContracts.containsKey(address.toLowerCase(Locale.US))) {
        for (int i = 0; i < config.getMultiSigAddresses().length; i++) {
          String altSig = signTransaction(transaction, config.getMultiSigAddresses()[i]);
          if (!altSig.isEmpty()) {
            // It signed it. But now we have to update the data and sign that.
            // Parse the data
            ContractInformation contractInfo =
                msigContracts.get(reverseMsigContracts.get(address.toLowerCase(Locale.US)));
            MultiSigContractInterface contract =
                (MultiSigContractInterface) contractInfo.getContractVersion().newInstance();
            LOGGER.debug("Signing transaction for contract version: "
                + contract.getClass().getCanonicalName());

            MultiSigContractParametersInterface contractParams = contract.getContractParameters();
            contractParams.decode(decodedTransaction.getData().getDecodedContents());

            String hashBytes = String.format("%64s", "0").replace(' ', '0');
            for (int j = 0; j < contractParams.getAddress().size(); j++) {
              String addressString =
                  String.format("%40s", contractParams.getAddress().get(j)).replace(' ', '0');
              hashBytes = hashBytes + addressString.substring(addressString.length() - 40);
              hashBytes +=
                  String
                      .format("%64s",
                          ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(
                              contractParams.getValue().get(j).toByteArray())))
                      .replace(' ', '0')
                      + String
                          .format("%64s",
                              ByteUtilities.toHexString(ByteUtilities
                                  .stripLeadingNullBytes(contractParams.getNonce().toByteArray())))
                          .replace(' ', '0');
              hashBytes = EthereumTools.hashSha3(hashBytes);
            }

            // Sign it and rebuild the data
            byte[][] sigData = signData(hashBytes, config.getMultiSigAddresses()[i], null);
            if (sigData.length < 3) {
              return transaction;
            }

            contractParams.getSigR().add(new BigInteger(1, sigData[0]));
            contractParams.getSigS().add(new BigInteger(1, sigData[1]));
            contractParams.getSigV().add(new BigInteger(1, sigData[2]));

            decodedTransaction.getData().setDecodedContents(contractParams.encode());

            // Sign the whole transaction again
            altSig = signTransaction(ByteUtilities.toHexString(decodedTransaction.encode()),
                config.getMultiSigAddresses()[i]);

            return altSig;
          }
        }
      } else if (name != null && reverseMsigContracts.containsKey(address.toLowerCase(Locale.US))) {
        // Update the data before we sign.
        // Parse the data
        ContractInformation contractInfo =
            msigContracts.get(reverseMsigContracts.get(address.toLowerCase(Locale.US)));
        MultiSigContractInterface contract =
            (MultiSigContractInterface) contractInfo.getContractVersion().newInstance();
        LOGGER.debug(
            "Signing transaction for contract version: " + contract.getClass().getCanonicalName());

        MultiSigContractParametersInterface contractParams = contract.getContractParameters();
        contractParams.decode(decodedTransaction.getData().getDecodedContents());
        String hashBytes = String.format("%64s", "0").replace(' ', '0');
        for (int j = 0; j < contractParams.getAddress().size(); j++) {
          String addressString =
              String.format("%40s", contractParams.getAddress().get(j)).replace(' ', '0');
          hashBytes = hashBytes + addressString.substring(addressString.length() - 40);
          hashBytes += String
              .format("%64s",
                  ByteUtilities.toHexString(ByteUtilities
                      .stripLeadingNullBytes(contractParams.getValue().get(j).toByteArray())))
              .replace(' ', '0')
              + String
                  .format("%64s",
                      ByteUtilities.toHexString(ByteUtilities
                          .stripLeadingNullBytes(contractParams.getNonce().toByteArray())))
                      .replace(' ', '0');
          hashBytes = EthereumTools.hashSha3(hashBytes);
        }

        // Sign it and rebuild the data
        String translatedAddress = reverseMsigContracts.get(address.toLowerCase(Locale.US));
        byte[][] sigData = signData(hashBytes, translatedAddress, name);
        if (sigData.length < 3) {
          return transaction;
        }

        contractParams.getSigR().add(new BigInteger(1, sigData[0]));
        contractParams.getSigS().add(new BigInteger(1, sigData[1]));
        contractParams.getSigV().add(new BigInteger(1, sigData[2]));

        decodedTransaction.getData().setDecodedContents(contractParams.encode());
        return signTransaction(ByteUtilities.toHexString(decodedTransaction.encode()),
            translatedAddress, name);
      }

      // Get the sigHash.
      RawTransaction sigTx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));

      String txCount =
          ethereumRpc.eth_getTransactionCount("0x" + address, DefaultBlock.LATEST.toString());
      BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount));
      if (nonce.equals(BigInteger.ZERO)) {
        sigTx.getNonce().setDecodedContents(new byte[] {});
      } else {
        sigTx.getNonce()
            .setDecodedContents(ByteUtilities.stripLeadingNullBytes(nonce.toByteArray()));
      }

      String sigString = ByteUtilities.toHexString(sigTx.getSigBytes());
      sigString = EthereumTools.hashSha3(sigString);

      byte[][] sigData = signData(sigString, address, name);
      if (sigData.length < 3) {
        return transaction;
      }

      sigTx.getSigR().setDecodedContents(sigData[0]);
      sigTx.getSigS().setDecodedContents(sigData[1]);
      sigTx.getSigV().setDecodedContents(sigData[2]);

      return ByteUtilities.toHexString(sigTx.encode());
    } catch (InstantiationException | IllegalAccessException e) {
      LOGGER.error(null, e);
      return null;
    }
  }

  private byte[][] signData(String data, String address, String name) {
    if (name == null) {
      // Catch errors here
      String sig = "";
      try {
        sig = ethereumRpc.eth_sign("0x" + address, data);
      } catch (Exception e) {
        LOGGER.warn(null, e);
        return new byte[0][0];
      }

      try {
        byte[] sigBytes = ByteUtilities.toByteArray(sig);
        byte[] sigR = Arrays.copyOfRange(sigBytes, 0, 32);
        byte[] sigS = Arrays.copyOfRange(sigBytes, 32, 64);
        byte[] sigV = Arrays.copyOfRange(sigBytes, 64, 65);
        sigV[0] += 27;

        return new byte[][] {sigR, sigS, sigV};
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
        sigR = signedBytes[0];
        sigS = signedBytes[1];
        sigV = signedBytes[2];

        if (sigV[0] != 0 && sigV[0] != 1) {
          continue;
        }

        signingAddress =
            ByteUtilities.toHexString(Secp256k1.recoverPublicKey(sigR, sigS, sigV, sigBytes));
        signingAddress = EthereumTools.getPublicAddress(signingAddress, false);
      } while (!address.equalsIgnoreCase(signingAddress));

      // Adjust for ethereum's encoding
      sigV[0] += 27;

      return new byte[][] {sigR, sigS, sigV};
    }
  }

  @Override
  public String sendTransaction(String transaction) {
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
            new String[] {ByteUtilities.toHexString(ByteUtilities.toByteArray(tx.getFrom()))});
        txDetail.setToAddress(
            new String[] {ByteUtilities.toHexString(ByteUtilities.toByteArray(tx.getTo()))});
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
            LOGGER.debug("Found transaction for contract version: "
                + contract.getClass().getCanonicalName());

            byte[] inputData = ByteUtilities.toByteArray(tx.getInput());
            MultiSigContractParametersInterface multiSig = contract.getContractParameters();
            multiSig.decode(inputData);

            if (multiSig.getFunction().equalsIgnoreCase(contract.getExecuteFunctionAddress())) {
              for (int j = 0; j < multiSig.getAddress().size(); j++) {
                TransactionDetails msigTx = new TransactionDetails();
                msigTx.setFromAddress(txDetail.getToAddress());
                msigTx.setToAddress(new String[] {multiSig.getAddress().get(j)});
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
      txHistory.get(address).forEach(detail -> {
        txDetails.add(detail);
      });
    }
    return txDetails.toArray(new TransactionDetails[] {});
  }

  @Override
  public TransactionDetails decodeRawTransaction(String transaction) {
    RawTransaction tx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    TransactionDetails txDetail = new TransactionDetails();
    txDetail.setAmount(new BigDecimal(new BigInteger(1, tx.getValue().getDecodedContents())));
    txDetail.setAmount(txDetail.getAmount().divide(BigDecimal.valueOf(config.getWeiMultiplier())));

    String senderKey =
        ByteUtilities.toHexString(Secp256k1.recoverPublicKey(tx.getSigR().getDecodedContents(),
            tx.getSigS().getDecodedContents(), tx.getSigV().getDecodedContents(),
            tx.getSigBytes()));
    String sender = EthereumTools.getPublicAddress(senderKey, false);
    txDetail.setFromAddress(new String[] {sender});

    txDetail
        .setToAddress(new String[] {ByteUtilities.toHexString(tx.getTo().getDecodedContents())});

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

        if (multiSig.getFunction().equalsIgnoreCase(contract.getExecuteFunctionAddress())) {
          txDetail.setFromAddress(txDetail.getToAddress());
          txDetail.setToAddress(new String[0]);
          txDetail.setAmount(BigDecimal.ZERO);
          for (int j = 0; j < multiSig.getAddress().size(); j++) {
            List<String> recipients = Arrays.asList(txDetail.getToAddress());
            recipients.add(multiSig.getAddress().get(j));
            txDetail.setToAddress(recipients.toArray(new String[0]));

            txDetail.setAmount(
                txDetail.getAmount().add(BigDecimal.valueOf(multiSig.getValue().get(j).longValue())
                    .divide(BigDecimal.valueOf(config.getWeiMultiplier()))));
          }
        }
      }
    } catch (InstantiationException | IllegalAccessException e) {
      LOGGER.debug("Unable to decode tx data", e);
    }

    return txDetail;
  }

  public ServerStatus getWalletStatus() {
    try {
      ethereumRpc.eth_blockNumber();
      return ServerStatus.CONNECTED;
    } catch (Exception e) {
      return ServerStatus.DISCONNECTED;
    }
  }
}
