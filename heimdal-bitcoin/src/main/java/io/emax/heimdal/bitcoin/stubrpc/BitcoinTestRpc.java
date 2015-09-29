package io.emax.heimdal.bitcoin.stubrpc;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Map;

import io.emax.heimdal.bitcoin.bitcoindrpc.AddressReceived;
import io.emax.heimdal.bitcoin.bitcoindrpc.AddressValidated;
import io.emax.heimdal.bitcoin.bitcoindrpc.BitcoindRpc;
import io.emax.heimdal.bitcoin.bitcoindrpc.Block;
import io.emax.heimdal.bitcoin.bitcoindrpc.BlockChainInfo;
import io.emax.heimdal.bitcoin.bitcoindrpc.DecodedTransaction;
import io.emax.heimdal.bitcoin.bitcoindrpc.InsuficientFundsException;
import io.emax.heimdal.bitcoin.bitcoindrpc.LastPayments;
import io.emax.heimdal.bitcoin.bitcoindrpc.MultiSig;
import io.emax.heimdal.bitcoin.bitcoindrpc.Outpoint;
import io.emax.heimdal.bitcoin.bitcoindrpc.OutpointDetails;
import io.emax.heimdal.bitcoin.bitcoindrpc.Output;
import io.emax.heimdal.bitcoin.bitcoindrpc.SigHash;
import io.emax.heimdal.bitcoin.bitcoindrpc.SignedTransaction;
import io.emax.heimdal.bitcoin.bitcoindrpc.DecodedTransaction.DecodedInput;

public class BitcoinTestRpc implements BitcoindRpc {

  @Override
  public Block getBlock(String blockHash) {
    return null;
  }

  @Override
  public BlockChainInfo getBlockChainInfo() {
    return null;
  }

  @Override
  public long getBlockCount() {
    return 0;
  }

  @Override
  public String getBlockHash(long blockHeight) {
    return null;
  }

  @Override
  public String help(String command) {
    return null;
  }

  @Override
  public void addNode(String node, AddNodeCommand command) {

  }

  @Override
  public String createrawtransaction(Outpoint[] unspentOutputs,
      Map<String, BigDecimal> addressAmounts) {
    return "deadbeefdeadbeefdeadbeefdeadbeef";
  }

  @Override
  public DecodedTransaction decoderawtransaction(String transaction) {
    DecodedTransaction tx = new DecodedTransaction();
    tx.setTransactionId("deadbeef");
    LinkedList<DecodedInput> inputs = new LinkedList<>();
    DecodedInput input = new DecodedInput();
    input.setAmount(BigDecimal.valueOf(150));
    input.setTransactionId("deadbeef");
    input.setOutputIndex(1);
    inputs.add(input);
    tx.setInputs(inputs);

    return tx;
  }

  @Override
  public String getrawtransaction(String transactionId) {
    return null;
  }

  @Override
  public String sendrawtransaction(String transaction, boolean allowHighFees) {    
    return "0x1234567890";
  }

  @Override
  public SignedTransaction signrawtransaction(String transaction, OutpointDetails[] outputs,
      String[] privateKeys, SigHash sigHash) {
    SignedTransaction signedTx = new SignedTransaction();
    signedTx.setTransaction(transaction + "1234");
    signedTx.setComplete(true);
    return signedTx;
  }

  @Override
  public MultiSig createMultiSig(int nrequired, String[] keys) {
    MultiSig mSig = new MultiSig();
    mSig.setAddress("2sjJ8zfnqZbkYj79EBtJLN4CDNPRg4s9xn");
    mSig.setRedeemScript("");
    return mSig;
  }

  @Override
  public BigDecimal estimateFee(int blocks) {
    return null;
  }

  @Override
  public AddressValidated validateAddress(String address) {
    return null;
  }

  @Override
  public String addmultisigaddress(int nrequired, String[] keys, String account) {
    return null;
  }

  @Override
  public MultiSig createmultisig(int nrequired, String[] keys) {
    MultiSig mSig = new MultiSig();
    mSig.setAddress("2sjJ8zfnqZbkYj79EBtJLN4CDNPRg4s9xn");
    mSig.setRedeemScript("");
    return mSig;
  }

  @Override
  public void backupWallet(String destination) {

  }

  @Override
  public String dumpPrivateKey(String address) {
    return null;
  }

  @Override
  public String getAccountAddress(String accountName) {
    return null;
  }

  @Override
  public String getAccount(String address) {
    return null;
  }

  @Override
  public String[] getaddressesbyaccount(String accountName) {
    return new String[] {};
  }

  @Override
  public String getnewaddress(String accountName) {
    return null;
  }

  @Override
  public void importaddress(String addressOrScript, String account, boolean rescan) {

  }

  @Override
  public void importprivkey(String privateKey, String account, boolean rescan) {

  }

  @Override
  public Map<String, BigDecimal> listAccounts(int confirmations, boolean includeWatchOnly) {
    return null;
  }

  @Override
  public AddressReceived[] listReceivedByAddress(int confirmations, boolean includeEmpty,
      boolean includeWatchOnly) {
    return null;
  }

  @Override
  public LastPayments listSinceBlock(String blockHash, int confirmations,
      boolean includeWatchOnly) {
    return null;
  }

  @Override
  public Output[] listunspent(int minimumConfirmations, int maximumConfirmations,
      String[] addresses) {
    LinkedList<Output> outputs = new LinkedList<>();
    for (String address : addresses) {
      Output output = new Output();
      output.setAccount("");
      output.setAddress(address);
      output.setAmount(BigDecimal.valueOf(30));
      output.setConfirmations(minimumConfirmations);
      output.setOutputIndex(1);
      output.setTransactionId("deadbeef");
      outputs.add(output);
    }
    Output[] outputArray = new Output[outputs.size()];
    return outputs.toArray(outputArray);
  }

  @Override
  public boolean lockUnspent(boolean lockOrUnlock, Outpoint[] unspentOutpoints) {
    return false;
  }

  @Override
  public String sendToAddress(String address, BigDecimal amount, String comment, String commentTo)
      throws InsuficientFundsException {
    return null;
  }

}
