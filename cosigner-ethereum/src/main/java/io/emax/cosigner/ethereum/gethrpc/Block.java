package io.emax.cosigner.ethereum.gethrpc;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Date;

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
  private Date timestamp;

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
  public Date getTimestamp() {
    if (timestamp == null) {
      return null;
    }
    return Date.from(timestamp.toInstant());
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = Date.from(timestamp.toInstant());
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
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((difficulty == null) ? 0 : difficulty.hashCode());
    result = prime * result + ((extraData == null) ? 0 : extraData.hashCode());
    result = prime * result + ((gasLimit == null) ? 0 : gasLimit.hashCode());
    result = prime * result + ((gasUsed == null) ? 0 : gasUsed.hashCode());
    result = prime * result + ((hash == null) ? 0 : hash.hashCode());
    result = prime * result + ((logsBloom == null) ? 0 : logsBloom.hashCode());
    result = prime * result + ((miner == null) ? 0 : miner.hashCode());
    result = prime * result + ((nonce == null) ? 0 : nonce.hashCode());
    result = prime * result + ((number == null) ? 0 : number.hashCode());
    result = prime * result + ((parentHash == null) ? 0 : parentHash.hashCode());
    result = prime * result + ((receiptRoot == null) ? 0 : receiptRoot.hashCode());
    result = prime * result + ((sha3Uncles == null) ? 0 : sha3Uncles.hashCode());
    result = prime * result + ((size == null) ? 0 : size.hashCode());
    result = prime * result + ((stateRoot == null) ? 0 : stateRoot.hashCode());
    result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
    result = prime * result + ((totalDifficulty == null) ? 0 : totalDifficulty.hashCode());
    result = prime * result + Arrays.hashCode(transactions);
    result = prime * result + ((transactionsRoot == null) ? 0 : transactionsRoot.hashCode());
    result = prime * result + Arrays.hashCode(uncles);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Block other = (Block) obj;
    if (difficulty == null) {
      if (other.difficulty != null) {
        return false;
      }
    } else if (!difficulty.equals(other.difficulty)) {
      return false;
    }
    if (extraData == null) {
      if (other.extraData != null) {
        return false;
      }
    } else if (!extraData.equals(other.extraData)) {
      return false;
    }
    if (gasLimit == null) {
      if (other.gasLimit != null) {
        return false;
      }
    } else if (!gasLimit.equals(other.gasLimit)) {
      return false;
    }
    if (gasUsed == null) {
      if (other.gasUsed != null) {
        return false;
      }
    } else if (!gasUsed.equals(other.gasUsed)) {
      return false;
    }
    if (hash == null) {
      if (other.hash != null) {
        return false;
      }
    } else if (!hash.equals(other.hash)) {
      return false;
    }
    if (logsBloom == null) {
      if (other.logsBloom != null) {
        return false;
      }
    } else if (!logsBloom.equals(other.logsBloom)) {
      return false;
    }
    if (miner == null) {
      if (other.miner != null) {
        return false;
      }
    } else if (!miner.equals(other.miner)) {
      return false;
    }
    if (nonce == null) {
      if (other.nonce != null) {
        return false;
      }
    } else if (!nonce.equals(other.nonce)) {
      return false;
    }
    if (number == null) {
      if (other.number != null) {
        return false;
      }
    } else if (!number.equals(other.number)) {
      return false;
    }
    if (parentHash == null) {
      if (other.parentHash != null) {
        return false;
      }
    } else if (!parentHash.equals(other.parentHash)) {
      return false;
    }
    if (receiptRoot == null) {
      if (other.receiptRoot != null) {
        return false;
      }
    } else if (!receiptRoot.equals(other.receiptRoot)) {
      return false;
    }
    if (sha3Uncles == null) {
      if (other.sha3Uncles != null) {
        return false;
      }
    } else if (!sha3Uncles.equals(other.sha3Uncles)) {
      return false;
    }
    if (size == null) {
      if (other.size != null) {
        return false;
      }
    } else if (!size.equals(other.size)) {
      return false;
    }
    if (stateRoot == null) {
      if (other.stateRoot != null) {
        return false;
      }
    } else if (!stateRoot.equals(other.stateRoot)) {
      return false;
    }
    if (timestamp == null) {
      if (other.timestamp != null) {
        return false;
      }
    } else if (!timestamp.equals(other.timestamp)) {
      return false;
    }
    if (totalDifficulty == null) {
      if (other.totalDifficulty != null) {
        return false;
      }
    } else if (!totalDifficulty.equals(other.totalDifficulty)) {
      return false;
    }
    if (!Arrays.equals(transactions, other.transactions)) {
      return false;
    }
    if (transactionsRoot == null) {
      if (other.transactionsRoot != null) {
        return false;
      }
    } else if (!transactionsRoot.equals(other.transactionsRoot)) {
      return false;
    }
    if (!Arrays.equals(uncles, other.uncles)) {
      return false;
    }
    return true;
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
