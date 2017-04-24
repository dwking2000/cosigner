package io.emax.cosigner.bitcoin.stubrpc;

import io.emax.cosigner.bitcoin.BitcoinConfiguration;
import io.emax.cosigner.bitcoin.BitcoinWallet;
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
import io.emax.cosigner.bitcoin.common.BitcoinTools;
import io.emax.cosigner.common.ByteUtilities;

import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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
    return "12abeed";
  }

  @Override
  public Map<String, Object> getBlock(String blockHash) {
    HashMap<String, Object> result = new HashMap<>();
    result.put("time", ((Long) (DateTime.now().getMillis() / 1000)).toString());
    return result;
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
    return account;
  }

  @Override
  public MultiSig createmultisig(int nrequired, String[] keys) {
    MultiSig multiSig = new MultiSig();
    multiSig.setAddress("2sjJ8zfnqZbkYj79EBtJLN4CDNPRg4s9xn");
    multiSig.setRedeemScript("");
    return multiSig;
  }

  @Override
  public String[] getaddressesbyaccount(String accountName) {
    return new String[]{};
  }

  @Override
  public String getnewaddress(String accountName) {
    return accountName;
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

    Output output = new Output();
    output.setAccount("");
    output.setAddress(new BitcoinWallet(new BitcoinConfiguration()).createAddress("deadbeef"));
    output.setAmount(BigDecimal.valueOf(30));
    output.setConfirmations(minimumConfirmations);
    output.setOutputIndex(1);
    output.setTransactionId("00000000000000000000000000000000000000000000000000000000deadbeef");
    String decodedAddress = BitcoinTools.decodeAddress(output.getAddress());
    byte[] addressBytes = ByteUtilities.toByteArray(decodedAddress);
    String scriptData = "76a914";
    scriptData += ByteUtilities.toHexString(addressBytes);
    scriptData += "88ac";
    output.setScriptPubKey(scriptData);

    return new Output[]{output};
  }

  @Override
  public Payment[] listtransactions(String account, int numberToReturn, int numberToSkip,
      boolean includeWatchOnly) {
    return new Payment[]{};
  }

  @Override
  public Map<String, Object> gettransaction(String txid, boolean includeWatchOnly) {
    return null;
  }
}
