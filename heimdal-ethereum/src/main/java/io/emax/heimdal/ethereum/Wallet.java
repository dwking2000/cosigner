package io.emax.heimdal.ethereum;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import io.emax.heimdal.ethereum.common.ByteUtilities;
import io.emax.heimdal.ethereum.common.DeterministicTools;
import io.emax.heimdal.ethereum.common.RLP;
import io.emax.heimdal.ethereum.common.RLPEntity;
import io.emax.heimdal.ethereum.common.RLPItem;
import io.emax.heimdal.ethereum.common.RLPList;
import io.emax.heimdal.ethereum.common.Secp256k1;
import io.emax.heimdal.ethereum.gethrpc.DefaultBlock;
import io.emax.heimdal.ethereum.gethrpc.EthereumRpc;

public class Wallet implements io.emax.heimdal.api.currency.Wallet {
  private EthereumRpc ethereumRpc = EthereumResource.getResource().getBitcoindRpc();
  private CurrencyConfiguration config = new CurrencyConfiguration();
  private static HashMap<String, Integer> addressRounds = new HashMap<>();

  @Override
  public String createAddress(String name) {
    // Generate the next private key
    int rounds = 1;
    if (addressRounds.containsKey(name)) {
      rounds = addressRounds.get(name);
    }
    addressRounds.put(name, rounds);
    String privateKey =
        DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);

    // Convert to an Ethereum address
    String publicAddress = DeterministicTools.getPublicAddress(privateKey);

    return publicAddress;
  }

  @Override
  public Iterable<String> getAddresses(String name) {
    // TODO Figure out how we're going to track multi-sig contracts without storing their address.
    int maxRounds = 1;
    if (addressRounds.containsKey(name)) {
      maxRounds = addressRounds.get(name);
    }

    LinkedList<String> addresses = new LinkedList<>();
    for (int i = 0; i < maxRounds; i++) {
      addresses.add(DeterministicTools.getPublicAddress(
          DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), i)));
    }

    return addresses;
  }

  @Override
  public String getMultiSigAddress(Iterable<String> addresses, String name) {
    // TODO Create a contract...
    return null;
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
    BigDecimal etherBalance = new BigDecimal(confirmedBalance).divide(BigDecimal.valueOf(config.getWeiMultiplier()));
    return etherBalance.toPlainString();
  }

  @Override
  public String createTransaction(Iterable<String> fromAddress, String toAddress,
      BigDecimal amount) {
    BigDecimal amountWei = amount.multiply(BigDecimal.valueOf(config.getWeiMultiplier()));

    // Create the transaction structure and serialize it
    RLPList tx = new RLPList();
    RLPItem nonce = new RLPItem();
    RLPItem gasPrice = new RLPItem(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
    RLPItem gasLimit = new RLPItem(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getSimpleTxGas()).toByteArray()));
    RLPItem to = new RLPItem(
        ByteUtilities.stripLeadingNullBytes(new BigInteger(toAddress, 16).toByteArray()));
    RLPItem value = new RLPItem(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(amountWei.longValue()).toByteArray()));
    RLPItem data = new RLPItem();

    tx.add(nonce);
    tx.add(gasPrice);
    tx.add(gasLimit);
    tx.add(to);
    tx.add(value);
    tx.add(data);

    return ByteUtilities.toHexString(tx.encode());
  }

  @Override
  public String signTransaction(String transaction, String address) {
    // Validate the transaction data
    RLPEntity decodedTransaction = RLP.parseArray(ByteUtilities.toByteArray(transaction));
    if (decodedTransaction == null || decodedTransaction.getClass() != RLPList.class
        || ((RLPList) decodedTransaction).size() < 6) {
      return "";
    }

    // Get the sigHash.
    // TODO create a TX class that knows how to do this.
    RLPList sigTx = new RLPList();
    sigTx.add(((RLPList) decodedTransaction).get(0)); // nonce
    sigTx.add(((RLPList) decodedTransaction).get(1)); // gasPrice
    sigTx.add(((RLPList) decodedTransaction).get(2)); // gasLimit
    sigTx.add(((RLPList) decodedTransaction).get(3)); // to
    sigTx.add(((RLPList) decodedTransaction).get(4)); // value
    sigTx.add(((RLPList) decodedTransaction).get(5)); // data

    String txCount =
        ethereumRpc.eth_getTransactionCount("0x" + address, DefaultBlock.LATEST.toString());
    BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount));
    nonce = nonce.add(BigInteger.ONE);
    sigTx.get(0).setDecodedContents(ByteUtilities.stripLeadingNullBytes(nonce.toByteArray()));

    String sigString = ByteUtilities.toHexString(sigTx.encode());
    sigString = DeterministicTools.hashSha3(sigString);
    String sig = ethereumRpc.eth_sign("0x" + address, sigString);
    byte[] sigBytes = ByteUtilities.toByteArray(sig);

    byte[] sigR = Arrays.copyOfRange(sigBytes, 0, 32);
    byte[] sigS = Arrays.copyOfRange(sigBytes, 32, 64);
    byte[] sigV = Arrays.copyOfRange(sigBytes, 64, 65);

    // convert recoveryId, because it has to be 27/28.
    sigV[0] += 27;
    RLPItem recId = new RLPItem(sigV);
    RLPItem r = new RLPItem(sigR);
    RLPItem s = new RLPItem(sigS);

    sigTx.add(recId);
    sigTx.add(r);
    sigTx.add(s);

    return ByteUtilities.toHexString(sigTx.encode());
  }

  @Override
  public String signTransaction(String transaction, String address, String name) {
    // Validate the transaction data
    RLPEntity decodedTransaction = RLP.parseArray(ByteUtilities.toByteArray(transaction));
    if (decodedTransaction == null || decodedTransaction.getClass() != RLPList.class
        || ((RLPList) decodedTransaction).size() < 6) {
      return "";
    }

    // Get the sigHash.
    // TODO create a TX class that knows how to do this.
    RLPList sigTx = new RLPList();
    sigTx.add(((RLPList) decodedTransaction).get(0)); // nonce
    sigTx.add(((RLPList) decodedTransaction).get(1)); // gasPrice
    sigTx.add(((RLPList) decodedTransaction).get(2)); // gasLimit
    sigTx.add(((RLPList) decodedTransaction).get(3)); // to
    sigTx.add(((RLPList) decodedTransaction).get(4)); // value
    sigTx.add(((RLPList) decodedTransaction).get(5)); // data

    String txCount =
        ethereumRpc.eth_getTransactionCount("0x" + address, DefaultBlock.LATEST.toString());
    BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount));
    if(nonce.equals(BigInteger.ZERO)) {
      sigTx.get(0).setDecodedContents(new byte[] {});
    } else {
      sigTx.get(0).setDecodedContents(ByteUtilities.stripLeadingNullBytes(nonce.toByteArray()));
    }

    String sigString = ByteUtilities.toHexString(sigTx.encode());
    sigString = DeterministicTools.hashSha3(sigString);

    // Determine the private key to use
    int rounds = 1;
    if (addressRounds.containsKey(name)) {
      rounds = addressRounds.get(name);
    }
    String privateKey = "";
    for (int i = 0; i < rounds; i++) {
      String privateKeyCheck =
          DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), i);
      if (DeterministicTools.getPublicAddress(privateKeyCheck).equals(address)) {
        privateKey = privateKeyCheck;
        break;
      }
    }
    if (privateKey == "") {
      return "";
    }

    // Sign and return it
    byte[] privateBytes = ByteUtilities.toByteArray(privateKey);
    byte[] sigBytes = ByteUtilities.toByteArray(sigString);
    byte[] sig = Secp256k1.signTransaction(sigBytes, privateBytes);

    byte[] recoveryId = Arrays.copyOfRange(sig, 0, 1);
    byte[] sigR = Arrays.copyOfRange(sig, 1, 33);
    byte[] sigS = Arrays.copyOfRange(sig, 33, 65);

    RLPItem recId = new RLPItem(recoveryId);
    RLPItem r = new RLPItem(sigR);
    RLPItem s = new RLPItem(sigS);

    sigTx.add(recId);
    sigTx.add(r);
    sigTx.add(s);

    return ByteUtilities.toHexString(sigTx.encode());
  }

  @Override
  public String sendTransaction(String transaction) {
    return ethereumRpc.eth_sendRawTransaction(transaction);
  }

}
