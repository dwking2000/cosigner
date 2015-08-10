package io.emax.heimdal.common;

/**
 * POJO for representing confirmations of balances. Timestamp is intended to be msecs since epoch
 * for simplicity.
 */
public class BalanceConfirmation {
  private final long timestamp; // msecs since epoc
  private final long blockNumber;
  private final String blockHash;

  public BalanceConfirmation(final long timestamp, final long blockNumber, String blockHash) {
    this.timestamp = timestamp; // msecs since epoc
    this.blockNumber = blockNumber;
    this.blockHash = blockHash;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getBlockNumber() {
    return blockNumber;
  }

  public String getBlockHash() {
    return blockHash;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((blockHash == null) ? 0 : blockHash.hashCode());
    result = prime * result + (int) (blockNumber ^ (blockNumber >>> 32));
    result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    BalanceConfirmation other = (BalanceConfirmation) obj;
    if (blockHash == null) {
      if (other.blockHash != null)
        return false;
    } else if (!blockHash.equals(other.blockHash))
      return false;
    if (blockNumber != other.blockNumber)
      return false;
    if (timestamp != other.timestamp)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "BalanceConfirmation [timestamp=" + timestamp + ", blockNumber=" + blockNumber
        + ", blockHash=" + blockHash + "]";
  }
}
