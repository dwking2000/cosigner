package io.emax.cosigner.ethereum.tokenstorage;

import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.common.crypto.Secp256k1;
import io.emax.cosigner.ethereum.core.common.EthereumTools;
import io.emax.cosigner.ethereum.core.gethrpc.DefaultBlock;
import io.emax.cosigner.ethereum.core.gethrpc.RawTransaction;
import io.emax.cosigner.ethereum.tokenstorage.contract.ContractInterface;
import io.emax.cosigner.ethereum.tokenstorage.contract.ContractParametersInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static io.emax.cosigner.ethereum.tokenstorage.Base.ethereumRpc;

/**
 * Signature methods
 */
public class Signatures {
  private static final Logger LOGGER = LoggerFactory.getLogger(Signatures.class);

  static Iterable<Iterable<String>> getSigString(String transaction, String address,
      boolean ignoreContractCode, Configuration config) {
    RawTransaction tx = RawTransaction.parseBytes(ByteUtilities.toByteArray(transaction));
    LinkedList<Iterable<String>> sigStrings = new LinkedList<>();

    if (tx == null) {
      LOGGER.warn("Not able to parse tx.");
      LinkedList<String> txData = new LinkedList<>();
      txData.add(transaction);
      LinkedList<Iterable<String>> wrappedTxData = new LinkedList<>();
      wrappedTxData.add(txData);
      return wrappedTxData;
    }

    // We were told not to break out the contract code for offline-style signing,
    // and simply sign the overall transaction
    if (!ignoreContractCode) {
      LOGGER.debug("Attempting to parse contract code...");
      String hashBytes;

      // Get the transaction data
      ContractInterface txContractInterface = Utilities
          .getContractVersion(ByteUtilities.toHexString(tx.getTo().getDecodedContents()), config);

      LOGGER.debug("Found contract interface: " + (txContractInterface == null ? "null" :
          txContractInterface.getClass().getCanonicalName()) + " at " + ByteUtilities
          .toHexString(tx.getTo().getDecodedContents()));

      if (txContractInterface != null) {
        Map<String, List<String>> contractParams = txContractInterface.getContractParameters()
            .parseTransfer(ByteUtilities.toHexString(tx.getData().getDecodedContents()));
        // If the parse failed to return anything then it's not one of our contract transfers.
        if (contractParams != null) {
          LOGGER.debug(Json.stringifyObject(Map.class, contractParams));

          BigInteger nonce =
              new BigInteger(contractParams.get(ContractParametersInterface.NONCE).get(0));
          String sender = contractParams.get(ContractParametersInterface.SENDER).get(0);
          List<String> recipients = contractParams.get(ContractParametersInterface.RECIPIENTS);
          List<String> amounts = contractParams.get(ContractParametersInterface.AMOUNT);

          // Hash to sign is hash(previous hash + recipient + amount + nonce)
          hashBytes = txContractInterface.getContractParameters()
              .calculateTxHash(config.getStorageContractAddress(), nonce.longValue(), sender,
                  recipients, amounts);
          LOGGER.debug("Got Offline-Transfer Hash of: " + hashBytes);
          LinkedList<String> msigString = new LinkedList<>();
          msigString.add(txContractInterface.getClass().getCanonicalName());
          msigString.add(hashBytes);
          sigStrings.add(msigString);
        } else {
          // Check if it's an admin function
          contractParams = txContractInterface.getContractParameters()
              .parseAdminFunction(ByteUtilities.toHexString(tx.getData().getDecodedContents()));
          // If it's null, then we couldn't determine what function it is.
          if (contractParams != null) {
            // Sign it as an admin function
            BigInteger nonce =
                new BigInteger(contractParams.get(ContractParametersInterface.NONCE).get(0));

            hashBytes = txContractInterface.getContractParameters().calculateAdminHash(ethereumRpc,
                ByteUtilities.toHexString(tx.getTo().getDecodedContents()), nonce.longValue());
            LOGGER.debug("Result: " + hashBytes);
            LinkedList<String> msigString = new LinkedList<>();
            msigString.add(txContractInterface.getClass().getCanonicalName());
            msigString.add(hashBytes);
            sigStrings.add(msigString);
          }
        }
      }
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

  static Iterable<Iterable<String>> signWithPrivateKey(Iterable<Iterable<String>> data,
      String privateKey, String address, Configuration config) {
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
      // Sign contract code if there is any
      if (contractData.size() > 0) {
        LOGGER.debug("Reading mSig data");
        String sigBytes = contractData.getLast();
        byte[][] sigData = signData(sigBytes, address, privateKey, config);
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

      // Couldn't parse the transaction, it's not a valid TX. Return no signature.
      if (rawTx == null) {
        LinkedList<String> signature = new LinkedList<>();
        signature.add(txData.getFirst());
        LinkedList<Iterable<String>> result = new LinkedList<>();
        result.add(signature);
        return result;
      }

      // If we've added mSig data then update the TX.
      // Get the contract that corresponds to the recipients code if possible.
      ContractInterface txContractInterface = Utilities
          .getContractVersion(ByteUtilities.toHexString(rawTx.getTo().getDecodedContents()),
              config);
      if (txContractInterface == null) {
        txContractInterface = config.getContractInterface();
      }
      if (signedData.size() > 0 && txContractInterface.getContractParameters()
          .parseTransfer(ByteUtilities.toHexString(rawTx.getData().getDecodedContents())) != null) {
        // There are new signatures and the TX appears to be a offlineTransfer
        rawTx = rebuildOfflineTransfer(contractData, rawTx, signedData);
      } else if (signedData.size() > 0 && txContractInterface.getContractParameters()
          .parseAdminFunction(ByteUtilities.toHexString(rawTx.getData().getDecodedContents()))
          != null) {
        // There are new signatures and the TX appears to be an admin function
        rawTx = rebuildAdminFunction(contractData, rawTx, signedData);
      }

      // Sign the TX itself so it can be broadcast.
      String txCount = txData.getLast();
      BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount));

      // RLP formatting quirks
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
      byte[][] sigData = signData(sigString, address, privateKey, config);
      if (sigData.length < 3) {
        // Signature data is bad, return the original transaction.
        LinkedList<String> signature = new LinkedList<>();
        signature.add(txData.getFirst());
        LinkedList<Iterable<String>> result = new LinkedList<>();
        result.add(signature);
        return result;
      }

      // Apply the signature to the tx structure.
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
      // Something went very wrong, return the original transaction.
      LOGGER.warn(null, e);
      LinkedList<String> signature = new LinkedList<>();
      signature.add(txData.getFirst());
      LinkedList<Iterable<String>> result = new LinkedList<>();
      result.add(signature);
      return result;
    }
  }

  static RawTransaction rebuildOfflineTransfer(LinkedList<String> contractData,
      RawTransaction rawTx, LinkedList<Iterable<String>> signedData)
      throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    // Load the right contract version so we put the data in the right places.
    String contractVersion = contractData.getFirst();
    ContractInterface contract =
        (ContractInterface) ContractInterface.class.getClassLoader().loadClass(contractVersion)
            .newInstance();
    ContractParametersInterface contractParms = contract.getContractParameters();
    Map<String, List<String>> contractParamData = contractParms
        .parseTransfer(ByteUtilities.toHexString(rawTx.getData().getDecodedContents()));

    // Append the signature data to data structures
    Iterator<String> msigSig = signedData.getFirst().iterator();
    contractParamData.get(ContractParametersInterface.SIGR).add(msigSig.next());
    contractParamData.get(ContractParametersInterface.SIGS).add(msigSig.next());
    contractParamData.get(ContractParametersInterface.SIGV).add(msigSig.next());

    // Convert all the components into values we can pass into the offlineTransfer call
    Long nonce =
        new BigInteger(contractParamData.get(ContractParametersInterface.NONCE).get(0)).longValue();
    String sender = contractParamData.get(ContractParametersInterface.SENDER).get(0);
    List<String> recipients = contractParamData.get(ContractParametersInterface.RECIPIENTS);
    List<BigInteger> amounts = new LinkedList<>();
    for (String amount : contractParamData.get(ContractParametersInterface.AMOUNT)) {
      amounts.add(new BigInteger(amount));
    }
    List<String> sigV = contractParamData.get(ContractParametersInterface.SIGV);
    List<String> sigR = contractParamData.get(ContractParametersInterface.SIGR);
    List<String> sigS = contractParamData.get(ContractParametersInterface.SIGS);

    // Rebuild the function data
    rawTx.getData().setDecodedContents(ByteUtilities.toByteArray(
        contractParms.offlineTransfer(nonce, sender, recipients, amounts, sigV, sigR, sigS)));

    return rawTx;
  }

  static RawTransaction rebuildAdminFunction(LinkedList<String> contractData, RawTransaction rawTx,
      LinkedList<Iterable<String>> signedData)
      throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    // Get the right contract version.
    String contractVersion = contractData.getFirst();
    ContractInterface contract =
        (ContractInterface) ContractInterface.class.getClassLoader().loadClass(contractVersion)
            .newInstance();
    ContractParametersInterface contractParms = contract.getContractParameters();

    Map<String, List<String>> contractParamData = contractParms
        .parseAdminFunction(ByteUtilities.toHexString(rawTx.getData().getDecodedContents()));

    // Update the contract data with the new signatures.
    Iterator<String> msigSig = signedData.getFirst().iterator();
    contractParamData.get(ContractParametersInterface.SIGR).add(msigSig.next());
    contractParamData.get(ContractParametersInterface.SIGS).add(msigSig.next());
    contractParamData.get(ContractParametersInterface.SIGV).add(msigSig.next());

    // Rebuild the function data
    rawTx.getData().setDecodedContents(
        ByteUtilities.toByteArray(contractParms.rebuildAdminFunction(contractParamData)));

    return rawTx;
  }

  /**
   * Attempt to sign the data with the provided information
   *
   * @param data       The data to be signed, we expect it to already be hashed.
   * @param address    The address the data is being signed for. Required when passing in a userKey
   *                   so we can determine which privateKey is being used.
   * @param signingKey The userKey or privateKey being used to sign. The method will attempt to
   *                   determine which type of key is in use based on the address provided.
   * @return Returns the signature data as a byte array in the format {R,S,V}. If signing fails it
   * will return an empty array.
   */
  static byte[][] signData(String data, String address, String signingKey, Configuration config) {
    if (signingKey == null) {
      return signWith3rdParty(data, address, signingKey, config);
    } else {
      int maxRounds = 100;

      // Attempt to recover the address as if it's a user key.
      String privateKey = "";
      if (address != null) {
        for (int i = 1; i <= maxRounds; i++) {
          String privateKeyCheck =
              EthereumTools.getDeterministicPrivateKey(signingKey, config.getServerPrivateKey(), i);
          if (EthereumTools.getPublicAddress(privateKeyCheck).equalsIgnoreCase(address)) {
            privateKey = privateKeyCheck;
            break;
          }
        }
        // If we couldn't match the address, assume the signingKey is actually a private key
        if (privateKey.isEmpty()) {
          privateKey = signingKey;
          address = EthereumTools.getPublicAddress(privateKey);
        }
      } else {
        // If the address is empty assume the signingKey is a private key and generate it from that.
        privateKey = signingKey;
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
          LOGGER.debug("Couldn't recover the public key");
          LOGGER.trace(null, e);
        }
        signingAddress = EthereumTools.getPublicAddress(signingAddress, false);
      } while (!address.equalsIgnoreCase(signingAddress));

      // Adjust for ethereum's encoding
      sigV[0] += 27;

      return new byte[][]{sigR, sigS, sigV};
    }
  }

  /**
   * Sign the data using a 3rd party signer.
   */
  static byte[][] signWith3rdParty(String data, String address, String signingKey,
      Configuration config) {
    return signWithEthNode(data, address);
  }

  /**
   * Attempt to sign the data with Ethereum node. I.E. Geth, Parity.
   */
  static byte[][] signWithEthNode(String data, String address) {
    String sig;
    try {
      LOGGER.debug("Asking geth to sign 0x" + data + " for 0x" + address);
      sig = ethereumRpc.eth_sign("0x" + address, "0x" + data);
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
        LOGGER.debug("Couldn't recover public key from signature");
        LOGGER.trace(null, e);
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
  }

}
