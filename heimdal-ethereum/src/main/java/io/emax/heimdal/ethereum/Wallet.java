package io.emax.heimdal.ethereum;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.bouncycastle.crypto.digests.SHA3Digest;

import io.emax.heimdal.ethereum.common.DeterministicTools;
import io.emax.heimdal.ethereum.common.Secp256k1;
import io.emax.heimdal.ethereum.gethrpc.DefaultBlock;
import io.emax.heimdal.ethereum.gethrpc.EthereumRpc;

public class Wallet implements io.emax.heimdal.api.currency.Wallet {
  private EthereumRpc ethereumRpc = EthereumResource.getResource().getBitcoindRpc();
  private CurrencyConfiguration config = new CurrencyConfiguration();
  
  @Override
  public String createAddress(String name) {
    // TODO will need a way to increment these... would like to avoid doing it in memory
    String privateKey = DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), 1);
    //String publicKey = DeterministicTools.getPublicKey(privateKey);
    String publicAddress = DeterministicTools.getPublicAddress(privateKey);
    
    return publicAddress;
  }

  @Override
  public Iterable<String> getAddresses(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getMultiSigAddress(Iterable<String> addresses, String name) {
    // TODO Create a contract... 
    return null;
  }

  @Override
  public String getBalance(String address) {    
    return new BigInteger("00" + ethereumRpc.eth_getBalance(address, DefaultBlock.latest.toString()).substring(2), 16).toString(10);          
  }

  @Override
  public String createTransaction(Iterable<String> fromAddress, String toAddress,
      BigDecimal amount) {
        // Get the accounts eth_getTransactionCount, use it for the nonce
        // Get the accounts balance, verify it's in range
        // Create the transaction structure and serialize it 
    return null;
  }

  @Override
  public String signTransaction(String transaction, String address) {
    
    // TODO this is a tech demo of the signing process, replace and move it.
    Secp256k1 crypto = new Secp256k1();
    byte[] data = new BigInteger("00" + transaction, 16).toByteArray();
    SHA3Digest md = new SHA3Digest(256);
    md.reset();
    md.update(data, 0, data.length);
    byte[] hashedData = new byte[32];
    md.doFinal(hashedData, 0);
    
    byte[] privateKey = new BigInteger("00" + address, 16).toByteArray();
    byte[] sig = crypto.signTransaction(hashedData, privateKey);
    
    return new BigInteger(1, sig).toString(16);
  }

  @Override
  public String signTransaction(String transaction, String address, String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String sendTransaction(String transaction) {
    // TODO Auto-generated method stub
    return null;
  }

}
