package io.emax.heimdal.bitcoin.bitcoind;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SignedTransaction {
  /**
   * The resulting serialized transaction encoded as hex with any signatures made inserted. If no
   * signatures were made, this will be the same transaction provided in parameter #1
   */
  @JsonProperty("hex")
  private String transaction;

  /**
   * The value true if transaction is fully signed; the value false if more signatures are required
   */
  @JsonProperty("complete")
  private boolean complete;

  public String getTransaction() {
    return transaction;
  }

  public void setTransaction(String transaction) {
    this.transaction = transaction;
  }

  public boolean isComplete() {
    return complete;
  }

  public void setComplete(boolean complete) {
    this.complete = complete;
  }

  @Override
  public String toString() {
    return "SignedTransaction [transaction=" + transaction + ", complete=" + complete + "]";
  }
}
