package io.emax.cosigner.bitcoin.stubrpc;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Map;

import io.emax.cosigner.bitcoin.bitcoindrpc.BitcoindRpc;
import io.emax.cosigner.bitcoin.bitcoindrpc.BlockChainInfo;
import io.emax.cosigner.bitcoin.bitcoindrpc.BlockChainName;
import io.emax.cosigner.bitcoin.bitcoindrpc.MultiSig;
import io.emax.cosigner.bitcoin.bitcoindrpc.Outpoint;
import io.emax.cosigner.bitcoin.bitcoindrpc.OutpointDetails;
import io.emax.cosigner.bitcoin.bitcoindrpc.Output;
import io.emax.cosigner.bitcoin.bitcoindrpc.Payment;
import io.emax.cosigner.bitcoin.bitcoindrpc.SigHash;
import io.emax.cosigner.bitcoin.bitcoindrpc.SignedTransaction;

public class BitcoinTestRpc implements BitcoindRpc {

  @Override
  public BlockChainInfo getblockchaininfo() {
    BlockChainInfo info = new BlockChainInfo();
    info.setChain(BlockChainName.regtest);
    return info;
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
  public String createrawtransaction(Outpoint[] unspentOutputs,
      Map<String, BigDecimal> addressAmounts) {
    return "deadbeefdeadbeefdeadbeefdeadbeef";
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
  public Map<String, BigDecimal> listaccounts(int confirmations, boolean includeWatchOnly) {
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
  public Payment[] listtransactions(String account, int numberToReturn, int numberToSkip,
      boolean includeWatchOnly) {
    return new Payment[] {};
  }
}
