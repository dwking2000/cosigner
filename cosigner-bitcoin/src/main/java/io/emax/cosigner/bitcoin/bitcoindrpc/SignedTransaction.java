package io.emax.heimdal.bitcoin.bitcoindrpc;

import java.util.Arrays;

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

  @JsonProperty("errors")
  private Errors[] errors;

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

  public Errors[] getErrors() {
    return errors;
  }

  public void setErrors(Errors[] errors) {
    this.errors = errors;
  }

  @Override
  public String toString() {
    return "SignedTransaction [transaction=" + transaction + ", complete=" + complete + ", errors="
        + Arrays.toString(errors) + "]";
  }

  public static class Errors {
    @JsonProperty("txid")
    private String txid;

    @JsonProperty("vout")
    private String vout;

    @JsonProperty("scriptSig")
    private String scriptSig;

    @JsonProperty("sequence")
    private String sequence;

    @JsonProperty("error")
    private String error;

    public String getTxid() {
      return txid;
    }

    public void setTxid(String txid) {
      this.txid = txid;
    }

    public String getVout() {
      return vout;
    }

    public void setVout(String vout) {
      this.vout = vout;
    }

    public String getScriptSig() {
      return scriptSig;
    }

    public void setScriptSig(String scriptSig) {
      this.scriptSig = scriptSig;
    }

    public String getSequence() {
      return sequence;
    }

    public void setSequence(String sequence) {
      this.sequence = sequence;
    }

    public String getError() {
      return error;
    }

    public void setError(String error) {
      this.error = error;
    }

    @Override
    public String toString() {
      return "Errors [txid=" + txid + ", vout=" + vout + ", scriptSig=" + scriptSig + ", sequence="
          + sequence + ", error=" + error + "]";
    }
  }
}
