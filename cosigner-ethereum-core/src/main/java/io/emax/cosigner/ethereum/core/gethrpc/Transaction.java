package io.emax.cosigner.ethereum.core.gethrpc;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Transaction {
  @JsonProperty("hash")
  private String hash;

  @JsonProperty("networkId")
  private String networkId;

  @JsonProperty("publicKey")
  private String publicKey;

  @JsonProperty("raw")
  private String raw;

  @JsonProperty("standardV")
  private String standardV;

  @JsonProperty("condition")
  private String condition;

  @JsonProperty("creates")
  private String creates;

  @JsonProperty("nonce")
  private String nonce;

  @JsonProperty("blockHash")
  private String blockHash;

  @JsonProperty("blockNumber")
  private String blockNumber;

  @JsonProperty("transactionIndex")
  private String transactionIndex;

  @JsonProperty("from")
  private String from;

  @JsonProperty("to")
  private String to;

  @JsonProperty("value")
  private String value;

  @JsonProperty("gasPrice")
  private String gasPrice;

  @JsonProperty("gas")
  private String gas;

  @JsonProperty("input")
  private String input;

  @JsonProperty("r")
  private String r;

  @JsonProperty("s")
  private String s;

  @JsonProperty("v")
  private String v;

  @JsonProperty("chainId")
  private String chainId;

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getNonce() {
    return nonce;
  }

  public void setNonce(String nonce) {
    this.nonce = nonce;
  }

  public String getBlockHash() {
    return blockHash;
  }

  public void setBlockHash(String blockHash) {
    this.blockHash = blockHash;
  }

  public String getBlockNumber() {
    return blockNumber;
  }

  public void setBlockNumber(String blockNumber) {
    this.blockNumber = blockNumber;
  }

  public String getTransactionIndex() {
    return transactionIndex;
  }

  public void setTransactionIndex(String transactionIndex) {
    this.transactionIndex = transactionIndex;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getGasPrice() {
    return gasPrice;
  }

  public void setGasPrice(String gasPrice) {
    this.gasPrice = gasPrice;
  }

  public String getGas() {
    return gas;
  }

  public void setGas(String gas) {
    this.gas = gas;
  }

  public String getInput() {
    return input;
  }

  public void setInput(String input) {
    this.input = input;
  }

  public String getR() {
    return r;
  }

  public void setR(String r) {
    this.r = r;
  }

  public String getS() {
    return s;
  }

  public void setS(String s) {
    this.s = s;
  }

  public String getV() {
    return v;
  }

  public void setV(String v) {
    this.v = v;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((blockHash == null) ? 0 : blockHash.hashCode());
    result = prime * result + ((blockNumber == null) ? 0 : blockNumber.hashCode());
    result = prime * result + ((from == null) ? 0 : from.hashCode());
    result = prime * result + ((gas == null) ? 0 : gas.hashCode());
    result = prime * result + ((gasPrice == null) ? 0 : gasPrice.hashCode());
    result = prime * result + ((hash == null) ? 0 : hash.hashCode());
    result = prime * result + ((input == null) ? 0 : input.hashCode());
    result = prime * result + ((nonce == null) ? 0 : nonce.hashCode());
    result = prime * result + ((to == null) ? 0 : to.hashCode());
    result = prime * result + ((transactionIndex == null) ? 0 : transactionIndex.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
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
    Transaction other = (Transaction) obj;
    if (blockHash == null) {
      if (other.blockHash != null) {
        return false;
      }
    } else if (!blockHash.equals(other.blockHash)) {
      return false;
    }
    if (blockNumber == null) {
      if (other.blockNumber != null) {
        return false;
      }
    } else if (!blockNumber.equals(other.blockNumber)) {
      return false;
    }
    if (from == null) {
      if (other.from != null) {
        return false;
      }
    } else if (!from.equals(other.from)) {
      return false;
    }
    if (gas == null) {
      if (other.gas != null) {
        return false;
      }
    } else if (!gas.equals(other.gas)) {
      return false;
    }
    if (gasPrice == null) {
      if (other.gasPrice != null) {
        return false;
      }
    } else if (!gasPrice.equals(other.gasPrice)) {
      return false;
    }
    if (hash == null) {
      if (other.hash != null) {
        return false;
      }
    } else if (!hash.equals(other.hash)) {
      return false;
    }
    if (input == null) {
      if (other.input != null) {
        return false;
      }
    } else if (!input.equals(other.input)) {
      return false;
    }
    if (nonce == null) {
      if (other.nonce != null) {
        return false;
      }
    } else if (!nonce.equals(other.nonce)) {
      return false;
    }
    if (to == null) {
      if (other.to != null) {
        return false;
      }
    } else if (!to.equals(other.to)) {
      return false;
    }
    if (transactionIndex == null) {
      if (other.transactionIndex != null) {
        return false;
      }
    } else if (!transactionIndex.equals(other.transactionIndex)) {
      return false;
    }
    if (value == null) {
      if (other.value != null) {
        return false;
      }
    } else if (!value.equals(other.value)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Transaction{" + "hash='" + hash + '\'' + ", networkId='" + networkId + '\''
        + ", publicKey='" + publicKey + '\'' + ", raw='" + raw + '\'' + ", standardV='" + standardV
        + '\'' + ", condition='" + condition + '\'' + ", creates='" + creates + '\'' + ", nonce='"
        + nonce + '\'' + ", blockHash='" + blockHash + '\'' + ", blockNumber='" + blockNumber + '\''
        + ", transactionIndex='" + transactionIndex + '\'' + ", from='" + from + '\'' + ", to='"
        + to + '\'' + ", value='" + value + '\'' + ", gasPrice='" + gasPrice + '\'' + ", gas='"
        + gas + '\'' + ", input='" + input + '\'' + ", r='" + r + '\'' + ", s='" + s + '\''
        + ", v='" + v + '\'' + '}';
  }

  public String getCreates() {
    return creates;
  }

  public void setCreates(String creates) {
    this.creates = creates;
  }

  public String getCondition() {
    return condition;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  public String getNetworkId() {
    return networkId;
  }

  public void setNetworkId(String networkId) {
    this.networkId = networkId;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  public String getRaw() {
    return raw;
  }

  public void setRaw(String raw) {
    this.raw = raw;
  }

  public String getStandardV() {
    return standardV;
  }

  public void setStandardV(String standardV) {
    this.standardV = standardV;
  }

  public String getChainId() {
    return chainId;
  }

  public void setChainId(String chainId) {
    this.chainId = chainId;
  }
}
