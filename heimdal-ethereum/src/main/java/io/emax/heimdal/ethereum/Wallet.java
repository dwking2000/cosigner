package io.emax.heimdal.ethereum;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import org.bouncycastle.crypto.digests.SHA3Digest;

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
  
  @Override
  public String createAddress(String name) {
    // TODO will need a way to increment these... would like to avoid doing it in memory
    String privateKey = DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), 1);
    String publicKey = DeterministicTools.getPublicKey(privateKey);
    System.out.println("Public key: " + publicKey);
    SHA3Digest md = new SHA3Digest(256);
    md.reset();
    byte[] pkBytes = DeterministicTools.getPublicKeyBytes(privateKey);
    md.update(pkBytes, 0, pkBytes.length);
    byte[] sha3Bytes = new byte[32];
    md.doFinal(sha3Bytes, 0);
    System.out.println("Sha3: " + DeterministicTools.toHexString(sha3Bytes));
    String publicAddress = DeterministicTools.getPublicAddress(privateKey);
    
    System.out.println("Private Key: " + privateKey);
    
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
    byte[] parsedBytes = new BigInteger("00" + address, 16).toByteArray();
    return DeterministicTools.toHexString(RLP.parseArray(parsedBytes[0] == 0 ? Arrays.copyOfRange(parsedBytes, 1, parsedBytes.length): parsedBytes).encode());
    
    //return new BigInteger("00" + ethereumRpc.eth_getBalance(address, DefaultBlock.latest.toString()).substring(2), 16).toString(10);          
  }

  @Override
  public String createTransaction(Iterable<String> fromAddress, String toAddress,
      BigDecimal amount) {
        // Get the accounts eth_getTransactionCount, use it for the nonce
        // Get the accounts balance, verify it's in range
        // Create the transaction structure and serialize it 
        RLPList tx = new RLPList();
        RLPItem nonce = new RLPItem(); nonce.setDecodedContents(new byte[] {0x01});
        RLPItem gasPrice = new RLPItem(); gasPrice.setDecodedContents(new BigInteger("09184E72A000", 16).toByteArray());
        RLPItem gasLimit = new RLPItem(); gasLimit.setDecodedContents(Arrays.copyOfRange(new BigInteger("B3", 16).toByteArray(), 1, 2));
        RLPItem to = new RLPItem(); to.setDecodedContents(Arrays.copyOfRange(new BigInteger("9bd67f9d2e6b12b9fee52b0071b53da27440a5b2", 16).toByteArray(), 1, 21));
        RLPItem value = new RLPItem(); value.setDecodedContents(new BigInteger("06F05B59D3B20000", 16).toByteArray());
        RLPItem data = new RLPItem(); data.setDecodedContents(new byte[] {});
        RLPItem recId = new RLPItem(); recId.setDecodedContents(new byte[] {0x1c});
        RLPItem sigR = new RLPItem(); sigR.setDecodedContents(new byte[32]);
        RLPItem sigS = new RLPItem(); sigS.setDecodedContents(new byte[32]);
        tx.add(nonce);
        tx.add(gasPrice);
        tx.add(gasLimit);
        tx.add(to);
        tx.add(value);
        tx.add(data);
        byte[] sigBytes = tx.encode();
        
        tx.add(recId);
        tx.add(sigR);
        tx.add(sigS);
        byte[] txBytes = tx.encode();
        
        System.out.println("Theoretical signing string: " + 
                           DeterministicTools.toHexString(sigBytes));
        
        SHA3Digest md = new SHA3Digest(256);
        md.reset();
        md.update(sigBytes, 0, sigBytes.length);
        byte[] digested = new byte[256/8];
        md.doFinal(digested, 0);
        
        System.out.println("SHA3 of signing string: " + DeterministicTools.toHexString(digested));
        
        return "Full TX: " + DeterministicTools.toHexString(txBytes);
  }

  @Override
  public String signTransaction(String transaction, String address) {
    
    // TODO this is a tech demo of the signing process, replace and move it.
    Secp256k1 crypto = new Secp256k1();
    byte[] data = new BigInteger("00" + transaction, 16).toByteArray();
    
    if(data.length > 0 && data[0] == 0) {
      data = Arrays.copyOfRange(data, 1, data.length);
    }
    SHA3Digest md = new SHA3Digest(256);
    md.reset();
    md.update(data, 0, data.length);
    byte[] hashedData = new byte[32];
    md.doFinal(hashedData, 0);
    
    System.out.println("Data: " + DeterministicTools.toHexString(data));
    System.out.println("Hashed Data: " + DeterministicTools.toHexString(hashedData));
    
    byte[] privateKey = new BigInteger("00" + address, 16).toByteArray();
    System.out.println("Pkey: " + DeterministicTools.toHexString(privateKey));
    byte[] sig = crypto.signTransaction(hashedData, privateKey);
    
    // Random sig test 383c22786240bc8a87cc5287d8e027039cd1baacc70eba1342a9307013f9e399
    byte[] gethR = new BigInteger("383c22786240bc8a87cc5287d8e027039cd1baacc70eba1342a9307013f9e399", 16).toByteArray();
    if(gethR.length > 0 && gethR[0] == 0) {
      gethR = Arrays.copyOfRange(gethR, 1, gethR.length);
    }
    byte[] gethS = new BigInteger("767b87ad8ebed3ac0b430e9679951bbc0ce3d1b4ce47f5f8f74473d8f3276342", 16).toByteArray();
    if(gethS.length > 0 && gethS[0] == 0) {
      gethS = Arrays.copyOfRange(gethS, 1, gethS.length);
    }
    byte[] message = new BigInteger("51989BF8A66621AD325781EC926E6D3FF6FF4016B21C1EBDD77780AD8D7234B2", 16).toByteArray();
    if(message.length > 0 && message[0] == 0) {
      message = Arrays.copyOfRange(message, 1, message.length);
    }
    int recId = 1;
    byte[] publicKey = crypto.recoverPublicKey(gethR, gethS, message, recId);
    md.reset();
    md.update(Arrays.copyOfRange(publicKey, 1, publicKey.length), 0, Arrays.copyOfRange(publicKey, 1, publicKey.length).length);
    byte[] recoveredHash = new byte[256/8];
    md.doFinal(recoveredHash, 0);
    System.out.println("recovered... " + DeterministicTools.toHexString(recoveredHash));    
    
    return DeterministicTools.toHexString(sig);
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
