package io.emax.heimdal.bitcoin.bitcoind;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author dquintela
 * 
 *         An object containing an array of transactions and the lastblock field
 */
public class LastPayments {
  /**
   * An array of objects each describing a particular payment to or from this wallet. The objects in
   * this array do not describe an actual transactions, so more than one object in this array may
   * come from the same transaction. This array may be empty
   * 
   * An payment which did not appear in the specified block or an earlier block
   */
  @JsonProperty("transactions")
  private List<Payment> payments;
  /**
   * The header hash of the block with the number of confirmations specified in the target
   * confirmations parameter, encoded as hex in RPC byte order
   */
  @JsonProperty("lastblock")
  private String lastBlockHash;

  @Override
  public String toString() {
    return "LastPayments [payments=" + payments + ", lastBlockHash=" + lastBlockHash + "]";
  }

  /**
   * @return the payments
   */
  public List<Payment> getPayments() {
    return payments;
  }

  /**
   * @param payments the payments to set
   */
  public void setPayments(List<Payment> payments) {
    this.payments = payments;
  }

  /**
   * @return the lastBlockHash
   */
  public String getLastBlockHash() {
    return lastBlockHash;
  }

  /**
   * @param lastBlockHash the lastBlockHash to set
   */
  public void setLastBlockHash(String lastBlockHash) {
    this.lastBlockHash = lastBlockHash;
  }
}
