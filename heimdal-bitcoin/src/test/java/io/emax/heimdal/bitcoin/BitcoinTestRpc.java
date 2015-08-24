package io.emax.heimdal.bitcoin;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Map;

import io.emax.heimdal.bitcoin.bitcoind.AddressReceived;
import io.emax.heimdal.bitcoin.bitcoind.AddressValidated;
import io.emax.heimdal.bitcoin.bitcoind.BitcoindRpc;
import io.emax.heimdal.bitcoin.bitcoind.Block;
import io.emax.heimdal.bitcoin.bitcoind.BlockChainInfo;
import io.emax.heimdal.bitcoin.bitcoind.DecodedTransaction;
import io.emax.heimdal.bitcoin.bitcoind.DecodedTransaction.DecodedInput;
import io.emax.heimdal.bitcoin.bitcoind.InsuficientFundsException;
import io.emax.heimdal.bitcoin.bitcoind.LastPayments;
import io.emax.heimdal.bitcoin.bitcoind.MultiSig;
import io.emax.heimdal.bitcoin.bitcoind.Outpoint;
import io.emax.heimdal.bitcoin.bitcoind.OutpointDetails;
import io.emax.heimdal.bitcoin.bitcoind.Output;
import io.emax.heimdal.bitcoin.bitcoind.SigHash;
import io.emax.heimdal.bitcoin.bitcoind.SignedTransaction;

public class BitcoinTestRpc implements BitcoindRpc {

  @Override
  public Block getBlock(String blockHash) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public BlockChainInfo getBlockChainInfo() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long getBlockCount() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getBlockHash(long blockHeight) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String help(String command) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void addNode(String node, AddNodeCommand command) {
    // TODO Auto-generated method stub

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
    // TODO Auto-generated method stub
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
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public AddressValidated validateAddress(String address) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String addmultisigaddress(int nrequired, String[] keys, String account) {
    // TODO Auto-generated method stub
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
    // TODO Auto-generated method stub

  }

  @Override
  public String dumpPrivateKey(String address) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getAccountAddress(String accountName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getAccount(String address) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String[] getaddressesbyaccount(String accountName) {
    return new String[] {};
  }

  @Override
  public String getnewaddress(String accountName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void importaddress(String addressOrScript, String account, boolean rescan) {
    // TODO Auto-generated method stub

  }

  @Override
  public void importprivkey(String privateKey, String account, boolean rescan) {
    // TODO Auto-generated method stub

  }

  @Override
  public Map<String, BigDecimal> listAccounts(int confirmations, boolean includeWatchOnly) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public AddressReceived[] listReceivedByAddress(int confirmations, boolean includeEmpty,
      boolean includeWatchOnly) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public LastPayments listSinceBlock(String blockHash, int confirmations,
      boolean includeWatchOnly) {
    // TODO Auto-generated method stub
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
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String sendToAddress(String address, BigDecimal amount, String comment, String commentTo)
      throws InsuficientFundsException {
    // TODO Auto-generated method stub
    return null;
  }

}
