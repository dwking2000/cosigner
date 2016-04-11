package io.emax.cosigner.ethereum.gethrpc;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class Block {
  @JsonProperty("number")
  private String number;

  @JsonProperty("hash")
  private String hash;

  @JsonProperty("parentHash")
  private String parentHash;

  @JsonProperty("nonce")
  private String nonce;

  @JsonProperty("sha3Uncles")
  private String sha3Uncles;

  @JsonProperty("logsBloom")
  private String logsBloom;

  @JsonProperty("transactionsRoot")
  private String transactionsRoot;

  @JsonProperty("stateRoot")
  private String stateRoot;

  @JsonProperty("receiptRoot")
  private String receiptRoot;

  @JsonProperty("miner")
  private String miner;

  @JsonProperty("difficulty")
  private String difficulty;

  @JsonProperty("totalDifficulty")
  private String totalDifficulty;

  @JsonProperty("extraData")
  private String extraData;

  @JsonProperty("size")
  private String size;

  @JsonProperty("gasLimit")
  private String gasLimit;

  @JsonProperty("gasUsed")
  private String gasUsed;

  @JsonProperty("timestamp")
  private String timestamp;

  @JsonProperty("transactions")
  private Transaction[] transactions = new Transaction[0];

  @JsonProperty("uncles")
  private String[] uncles = new String[0];

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getParentHash() {
    return parentHash;
  }

  public void setParentHash(String parentHash) {
    this.parentHash = parentHash;
  }

  public String getNonce() {
    return nonce;
  }

  public void setNonce(String nonce) {
    this.nonce = nonce;
  }

  public String getSha3Uncles() {
    return sha3Uncles;
  }

  public void setSha3Uncles(String sha3Uncles) {
    this.sha3Uncles = sha3Uncles;
  }

  public String getLogsBloom() {
    return logsBloom;
  }

  public void setLogsBloom(String logsBloom) {
    this.logsBloom = logsBloom;
  }

  public String getTransactionsRoot() {
    return transactionsRoot;
  }

  public void setTransactionsRoot(String transactionsRoot) {
    this.transactionsRoot = transactionsRoot;
  }

  public String getStateRoot() {
    return stateRoot;
  }

  public void setStateRoot(String stateRoot) {
    this.stateRoot = stateRoot;
  }

  public String getReceiptRoot() {
    return receiptRoot;
  }

  public void setReceiptRoot(String receiptsRoot) {
    this.receiptRoot = receiptsRoot;
  }

  public String getMiner() {
    return miner;
  }

  public void setMiner(String miner) {
    this.miner = miner;
  }

  public String getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(String difficulty) {
    this.difficulty = difficulty;
  }

  public String getTotalDifficulty() {
    return totalDifficulty;
  }

  public void setTotalDifficulty(String totalDifficulty) {
    this.totalDifficulty = totalDifficulty;
  }

  public String getExtraData() {
    return extraData;
  }

  public void setExtraData(String extraData) {
    this.extraData = extraData;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getGasLimit() {
    return gasLimit;
  }

  public void setGasLimit(String gasLimit) {
    this.gasLimit = gasLimit;
  }

  public String getGasUsed() {
    return gasUsed;
  }

  public void setGasUsed(String gasUsed) {
    this.gasUsed = gasUsed;
  }

  /**
   * Returns timestamp, null if null.
   */
  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Get the transactions that are present in this block.
   *
   * @return Array of transactions.
   */
  public Transaction[] getTransactions() {
    Transaction[] retArray = new Transaction[transactions.length];
    System.arraycopy(transactions, 0, retArray, 0, transactions.length);
    return retArray;
  }

  public void setTransactions(Transaction[] transactions) {
    this.transactions = new Transaction[transactions.length];
    System.arraycopy(transactions, 0, this.transactions, 0, transactions.length);
  }

  /**
   * Get the uncles that were present for this block.
   *
   * @return Array of block hashes of the uncles present for this block.
   */
  public String[] getUncles() {
    String[] retArray = new String[uncles.length];
    System.arraycopy(uncles, 0, retArray, 0, uncles.length);
    return retArray;
  }

  public void setUncles(String[] uncles) {
    this.uncles = new String[uncles.length];
    System.arraycopy(uncles, 0, this.uncles, 0, uncles.length);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    Block block = (Block) obj;

    if (number != null ? !number.equals(block.number) : block.number != null) {
      return false;
    }
    if (hash != null ? !hash.equals(block.hash) : block.hash != null) {
      return false;
    }
    if (parentHash != null ? !parentHash.equals(block.parentHash) : block.parentHash != null) {
      return false;
    }
    if (nonce != null ? !nonce.equals(block.nonce) : block.nonce != null) {
      return false;
    }
    if (sha3Uncles != null ? !sha3Uncles.equals(block.sha3Uncles) : block.sha3Uncles != null) {
      return false;
    }
    if (logsBloom != null ? !logsBloom.equals(block.logsBloom) : block.logsBloom != null) {
      return false;
    }
    if (transactionsRoot != null ? !transactionsRoot.equals(block.transactionsRoot) :
        block.transactionsRoot != null) {
      return false;
    }
    if (stateRoot != null ? !stateRoot.equals(block.stateRoot) : block.stateRoot != null) {
      return false;
    }
    if (receiptRoot != null ? !receiptRoot.equals(block.receiptRoot) : block.receiptRoot != null) {
      return false;
    }
    if (miner != null ? !miner.equals(block.miner) : block.miner != null) {
      return false;
    }
    if (difficulty != null ? !difficulty.equals(block.difficulty) : block.difficulty != null) {
      return false;
    }
    if (totalDifficulty != null ? !totalDifficulty.equals(block.totalDifficulty) :
        block.totalDifficulty != null) {
      return false;
    }
    if (extraData != null ? !extraData.equals(block.extraData) : block.extraData != null) {
      return false;
    }
    if (size != null ? !size.equals(block.size) : block.size != null) {
      return false;
    }
    if (gasLimit != null ? !gasLimit.equals(block.gasLimit) : block.gasLimit != null) {
      return false;
    }
    if (gasUsed != null ? !gasUsed.equals(block.gasUsed) : block.gasUsed != null) {
      return false;
    }
    if (timestamp != null ? !timestamp.equals(block.timestamp) : block.timestamp != null) {
      return false;
    }
    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    if (!Arrays.equals(transactions, block.transactions)) {
      return false;
    }
    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    return Arrays.equals(uncles, block.uncles);

  }

  @Override
  public int hashCode() {
    int result = number != null ? number.hashCode() : 0;
    result = 31 * result + (hash != null ? hash.hashCode() : 0);
    result = 31 * result + (parentHash != null ? parentHash.hashCode() : 0);
    result = 31 * result + (nonce != null ? nonce.hashCode() : 0);
    result = 31 * result + (sha3Uncles != null ? sha3Uncles.hashCode() : 0);
    result = 31 * result + (logsBloom != null ? logsBloom.hashCode() : 0);
    result = 31 * result + (transactionsRoot != null ? transactionsRoot.hashCode() : 0);
    result = 31 * result + (stateRoot != null ? stateRoot.hashCode() : 0);
    result = 31 * result + (receiptRoot != null ? receiptRoot.hashCode() : 0);
    result = 31 * result + (miner != null ? miner.hashCode() : 0);
    result = 31 * result + (difficulty != null ? difficulty.hashCode() : 0);
    result = 31 * result + (totalDifficulty != null ? totalDifficulty.hashCode() : 0);
    result = 31 * result + (extraData != null ? extraData.hashCode() : 0);
    result = 31 * result + (size != null ? size.hashCode() : 0);
    result = 31 * result + (gasLimit != null ? gasLimit.hashCode() : 0);
    result = 31 * result + (gasUsed != null ? gasUsed.hashCode() : 0);
    result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
    result = 31 * result + Arrays.hashCode(transactions);
    result = 31 * result + Arrays.hashCode(uncles);
    return result;
  }

  @Override
  public String toString() {
    return "Block [number=" + number + ", hash=" + hash + ", parentHash=" + parentHash + ", nonce="
        + nonce + ", sha3Uncles=" + sha3Uncles + ", logsBloom=" + logsBloom + ", transactionsRoot="
        + transactionsRoot + ", stateRoot=" + stateRoot + ", receiptsRoot=" + receiptRoot
        + ", miner=" + miner + ", difficulty=" + difficulty + ", totalDifficulty=" + totalDifficulty
        + ", extraData=" + extraData + ", size=" + size + ", gasLimit=" + gasLimit + ", gasUsed="
        + gasUsed + ", timestamp=" + timestamp + ", transactions=" + Arrays.toString(transactions)
        + ", uncles=" + Arrays.toString(uncles) + "]";
  }

}
