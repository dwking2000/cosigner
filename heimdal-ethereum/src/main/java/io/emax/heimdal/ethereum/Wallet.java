package io.emax.heimdal.ethereum;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import io.emax.heimdal.ethereum.common.ByteUtilities;
import io.emax.heimdal.ethereum.common.DeterministicTools;
import io.emax.heimdal.ethereum.common.RLPItem;
import io.emax.heimdal.ethereum.common.RLPList;
import io.emax.heimdal.ethereum.common.Secp256k1;
import io.emax.heimdal.ethereum.gethrpc.CallData;
import io.emax.heimdal.ethereum.gethrpc.DefaultBlock;
import io.emax.heimdal.ethereum.gethrpc.EthereumRpc;
import io.emax.heimdal.ethereum.gethrpc.MultiSigContract;
import io.emax.heimdal.ethereum.gethrpc.MultiSigContractParameters;
import io.emax.heimdal.ethereum.gethrpc.RawTransaction;
import rx.Observable;
import rx.Subscription;

public class Wallet implements io.emax.heimdal.api.currency.Wallet {
  private EthereumRpc ethereumRpc = EthereumResource.getResource().getGethRpc();
  private CurrencyConfiguration config = new CurrencyConfiguration();
  private static HashMap<String, Integer> addressRounds = new HashMap<>();
  private static HashMap<String, String> msigContracts = new HashMap<>();
  private static HashMap<String, String> reverseMsigContracts = new HashMap<>();
  private static Subscription multiSigSubscription;

  public Wallet(EthereumRpc rpc) {
    this.ethereumRpc = rpc;
    try {
      syncMultiSigAddresses();
    } catch (Exception e) {
      // this is ok.
    }

    if (multiSigSubscription == null) {
      multiSigSubscription = Observable.interval(1, TimeUnit.MINUTES).onErrorReturn(null)
          .subscribe(tick -> syncMultiSigAddresses());
    }
  }

  public Wallet() {
    try {
      syncMultiSigAddresses();
    } catch (Exception e) {
      // this is ok.
    }

    if (multiSigSubscription == null) {
      multiSigSubscription = Observable.interval(1, TimeUnit.MINUTES).onErrorReturn(null)
          .subscribe(tick -> syncMultiSigAddresses());
    }
  }

  private synchronized void syncMultiSigAddresses() {
    String contractPayload = "0x" + MultiSigContract.getContractPayload();
    String txCount = ethereumRpc.eth_getTransactionCount("0x" + config.getContractAccount(),
        DefaultBlock.LATEST.toString());
    int rounds = new BigInteger(1, ByteUtilities.toByteArray(txCount)).intValue();
    for (int i = 0; i < rounds; i++) {
      RLPList contractAddress = new RLPList();
      RLPItem contractCreator = new RLPItem(ByteUtilities.toByteArray(config.getContractAccount()));
      RLPItem nonce =
          new RLPItem(ByteUtilities.stripLeadingNullBytes(BigInteger.valueOf(i).toByteArray()));
      contractAddress.add(contractCreator);
      contractAddress.add(nonce);

      String contract = DeterministicTools
          .hashSha3(ByteUtilities.toHexString(contractAddress.encode())).substring(96 / 4, 256 / 4);
      String contractCode =
          ethereumRpc.eth_getCode("0x" + contract.toLowerCase(), DefaultBlock.LATEST.toString());

      if (contractCode.equalsIgnoreCase(contractPayload)) {
        // We found an existing contract
        CallData callData = new CallData();
        callData.setTo("0x" + contract);
        callData.setData("0x" + MultiSigContract.getGetOwnersFunctionAddress());
        callData.setGas("100000"); // Doesn't matter, just can't be nil
        callData.setGasprice("100000"); // Doesn't matter, just can't be nil
        String response = ethereumRpc.eth_call(callData, DefaultBlock.LATEST.toString());

        byte[] callBytes = ByteUtilities.toByteArray(response);
        int bufferPointer = 32; // skip first value, we know it just points to the next one.
        byte[] sizeBytes = Arrays.copyOfRange(callBytes, bufferPointer, bufferPointer + 32);
        bufferPointer += 32;
        int numAddresses = new BigInteger(1, sizeBytes).intValue();
        for (int j = 0; j < numAddresses; j++) {
          byte[] addressBytes = Arrays.copyOfRange(callBytes, bufferPointer, bufferPointer + 32);
          bufferPointer += 32;
          String userAddress =
              ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(addressBytes));
          userAddress = String.format("%40s", userAddress).replace(' ', '0');

          // Skip it if we already know about it
          if (reverseMsigContracts.containsKey(contract.toLowerCase())) {
            continue;
          }

          msigContracts.put(userAddress.toLowerCase(), contract.toLowerCase());
          reverseMsigContracts.put(contract.toLowerCase(), userAddress.toLowerCase());
        }
      }
    }

  }

  @Override
  public String createAddress(String name) {
    // Generate the next private key
    int rounds = 1;
    String privateKey =
        DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);

    // Convert to an Ethereum address
    String publicAddress = DeterministicTools.getPublicAddress(privateKey);

    while (msigContracts.containsKey(publicAddress.toLowerCase())) {
      rounds++;
      privateKey =
          DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);
      publicAddress = DeterministicTools.getPublicAddress(privateKey);
    }
    addressRounds.put(name, rounds);

    return publicAddress;
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
      addresses.add(DeterministicTools.getPublicAddress(
          DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), i)));
    }

    LinkedList<String> contracts = new LinkedList<>();
    for (String address : addresses) {
      if (msigContracts.containsKey(address.toLowerCase())) {
        contracts.add(msigContracts.get(address.toLowerCase()));
      }
    }

    return contracts;
  }

  @Override
  public String getMultiSigAddress(Iterable<String> addresses, String name) {
    // Look for existing msig account for this address.
    String userAddress = addresses.iterator().next().toLowerCase();
    if (msigContracts.containsKey(userAddress.toLowerCase())) {
      return msigContracts.get(userAddress).toLowerCase();
    }

    // Create the TX data structure
    RawTransaction tx = new RawTransaction();
    tx.getGasPrice().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
    tx.getGasLimit().setDecodedContents(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));

    // Setup parameters for contract
    String contractInit = MultiSigContract.getInitData();
    // Parameters for constructor are appended after the contract code
    // Each value is a 64-byte hex entry, one after the next with no delimiters
    // Addresses[] - because it's an array we provide a pointer relative to the input data start,
    // showing where you can find the data
    String accountOffset = String.format("%64s", "40").replace(' ', '0');
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
    String contractCode = contractInit + accountOffset + requiredSigs + numberOfAddresses;
    for (String addr : addressesUsed) {
      contractCode += addr;
    }
    tx.getData().setDecodedContents(ByteUtilities.toByteArray(contractCode));

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
    RLPList contractAddress = new RLPList();
    RLPItem contractCreator = new RLPItem(ByteUtilities.toByteArray(config.getContractAccount()));
    contractAddress.add(contractCreator);
    contractAddress.add(tx.getNonce());

    // Figure out the contract address and store it in lookup tables for future use
    String contract = DeterministicTools
        .hashSha3(ByteUtilities.toHexString(contractAddress.encode())).substring(96 / 4, 256 / 4);

    // Figure out if we already created this, if so, skip it...
    if (reverseMsigContracts.containsKey(contract.toLowerCase()))
      return "";

    // Otherwise, register it
    msigContracts.put(userAddress, contract.toLowerCase());
    reverseMsigContracts.put(contract.toLowerCase(), userAddress);

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
      if (msig.equalsIgnoreCase(senderAddress)) {
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
    if (isMsigSender) {
      // move things around to match the contract
      // Increase gas to contract amount
      tx.getGasLimit().setDecodedContents(ByteUtilities
          .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));

      // Clear the value, it's part of the data
      tx.getValue().setDecodedContents(new byte[] {});

      // data... caff27bb execute(address,uint256,uint256,uint8[],bytes32[],bytes32[])
      MultiSigContractParameters contractParms = new MultiSigContractParameters();
      contractParms.setFunction(MultiSigContract.getExecuteFunctionAddress());
      toAddress.iterator().forEachRemaining(rcpt -> {
        contractParms.getAddress().add(rcpt.getRecipientAddress());
        contractParms.getValue().add(rcpt.getAmount()
            .multiply(BigDecimal.valueOf(config.getWeiMultiplier())).toBigInteger());
      });

      // For this particular contract the previous nonce is stored at 0x0a.
      // It will only accept nonce's one greater than this.
      String txCount = ethereumRpc.eth_getStorageAt("0x" + senderAddress.toLowerCase(), "0x1",
          DefaultBlock.LATEST.toString());
      BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount)).add(BigInteger.ONE);
      contractParms.setNonce(nonce);

      tx.getData().setDecodedContents(contractParms.encode());

      // Change the recipient
      tx.getTo().setDecodedContents(
          ByteUtilities.stripLeadingNullBytes(new BigInteger(senderAddress, 16).toByteArray()));
    }

    return ByteUtilities.toHexString(tx.encode());
  }

  @Override
  public String signTransaction(String transaction, String address) {
    return signTransaction(transaction, address, null);
  }

  @Override
  public String signTransaction(String transaction, String address, String name) {
    // Validate the transaction data
    RawTransaction decodedTransaction =
        RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));

    // We've been asked to sign something for the multi-sig contract without a user-key
    // Going on the assumption that the local geth wallet only has one key
    if (name == null && reverseMsigContracts.containsKey(address.toLowerCase())) {
      for (int i = 0; i < config.getMultiSigAddresses().length; i++) {
        String altSig = signTransaction(transaction, config.getMultiSigAddresses()[i]);
        if (!altSig.isEmpty()) {
          // It signed it. But now we have to update the data and sign that.
          // Parse the data
          MultiSigContractParameters contractParams = new MultiSigContractParameters();
          contractParams.decode(decodedTransaction.getData().getDecodedContents());

          String hashBytes = String.format("%64s", "0").replace(' ', '0');
          for (int j = 0; j < contractParams.getAddress().size(); j++) {
            String addressString =
                String.format("%40s", contractParams.getAddress().get(j)).replace(' ', '0');
            hashBytes = hashBytes + addressString.substring(addressString.length() - 40);
            hashBytes +=
                String
                    .format("%64s",
                        ByteUtilities.toHexString(ByteUtilities
                            .stripLeadingNullBytes(contractParams.getValue().get(j).toByteArray())))
                .replace(' ', '0')
                + String
                    .format("%64s",
                        ByteUtilities.toHexString(ByteUtilities
                            .stripLeadingNullBytes(contractParams.getNonce().toByteArray())))
                        .replace(' ', '0');
            hashBytes = DeterministicTools.hashSha3(hashBytes);
          }

          System.out.println("Signing hash: " + hashBytes);

          // Sign it and rebuild the data
          byte[][] sigData = signData(hashBytes, config.getMultiSigAddresses()[i], name);
          if (sigData.length < 3)
            return transaction;

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
    } else if (name != null && reverseMsigContracts.containsKey(address.toLowerCase())) {
      String translatedAddress = reverseMsigContracts.get(address.toLowerCase());
      // Update the data before we sign.
      // Parse the data
      MultiSigContractParameters contractParams = new MultiSigContractParameters();
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
        hashBytes = DeterministicTools.hashSha3(hashBytes);
      }

      System.out.println("Signing hash: " + hashBytes);

      // Sign it and rebuild the data
      byte[][] sigData = signData(hashBytes, translatedAddress, name);
      if (sigData.length < 3)
        return transaction;

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
      sigTx.getNonce().setDecodedContents(ByteUtilities.stripLeadingNullBytes(nonce.toByteArray()));
    }

    String sigString = ByteUtilities.toHexString(sigTx.getSigBytes());
    sigString = DeterministicTools.hashSha3(sigString);

    byte[][] sigData = signData(sigString, address, name);
    if (sigData.length < 3)
      return transaction;

    sigTx.getSigR().setDecodedContents(sigData[0]);
    sigTx.getSigS().setDecodedContents(sigData[1]);
    sigTx.getSigV().setDecodedContents(sigData[2]);

    return ByteUtilities.toHexString(sigTx.encode());
  }

  private byte[][] signData(String data, String address, String name) {
    if (name == null) {
      // Catch errors here
      String sig = "";
      try {
        sig = ethereumRpc.eth_sign("0x" + address, data);
      } catch (Exception e) {
        return new byte[][] {};
      }

      try {
        byte[] sigBytes = ByteUtilities.toByteArray(sig);
        byte[] sigR = Arrays.copyOfRange(sigBytes, 0, 32);
        byte[] sigS = Arrays.copyOfRange(sigBytes, 32, 64);
        byte[] sigV = Arrays.copyOfRange(sigBytes, 64, 65);
        sigV[0] += 27;

        return new byte[][] {sigR, sigS, sigV};
      } catch (Exception e) {
        return new byte[][] {};
      }
    } else {
      int rounds = 1;
      if (addressRounds.containsKey(name)) {
        rounds = addressRounds.get(name);
      } else {
        // Generate rounds
        createAddress(name);
        rounds = addressRounds.get(name);
      }

      String privateKey = "";
      for (int i = 1; i <= rounds; i++) {
        String privateKeyCheck =
            DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), i);
        if (DeterministicTools.getPublicAddress(privateKeyCheck).equalsIgnoreCase(address)) {
          privateKey = privateKeyCheck;
          break;
        }
      }
      if (privateKey == "") {
        return new byte[][] {};
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
        byte[] signedBytes = Secp256k1.signTransaction(sigBytes, privateBytes);
        sigV = Arrays.copyOfRange(signedBytes, 0, 1);
        sigR = Arrays.copyOfRange(signedBytes, 1, 33);
        sigS = Arrays.copyOfRange(signedBytes, 33, 65);

        if (sigV[0] != 27 && sigV[0] != 28)
          continue;

        signingAddress = ByteUtilities
            .toHexString(Secp256k1.recoverPublicKey(sigR, sigS, sigBytes, sigV[0] - 27));
        signingAddress = DeterministicTools.getPublicAddress(signingAddress, false);
      } while (!address.equalsIgnoreCase(signingAddress));

      return new byte[][] {sigR, sigS, sigV};
    }
  }

  @Override
  public String sendTransaction(String transaction) {
    return ethereumRpc.eth_sendRawTransaction(transaction);
  }

}
