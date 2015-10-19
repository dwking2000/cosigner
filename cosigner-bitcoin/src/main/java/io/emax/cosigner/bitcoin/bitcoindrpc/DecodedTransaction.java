package io.emax.cosigner.bitcoin.bitcoindrpc;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DecodedTransaction {
  /**
   * The transaction’s TXID encoded as hex in RPC byte order
   */
  @JsonProperty("txid")
  private String transactionId;

  /**
   * The transaction format version number
   */
  @JsonProperty("version")
  private long version;

  /**
   * The transaction’s locktime: either a Unix epoch date or block height; see the Locktime parsing
   * rules
   */
  @JsonProperty("locktime")
  private long locktime;

  /**
   * An array of objects with each object being an input vector (vin) for this transaction. Input
   * objects will have the same order within the array as they have in the transaction, so the first
   * input listed will be input 0
   */
  @JsonProperty("vin")
  private List<DecodedInput> inputs;

  /**
   * An array of objects each describing an output vector (vout) for this transaction. Output
   * objects will have the same order within the array as they have in the transaction, so the first
   * output listed will be output 0
   */
  @JsonProperty("vout")
  private List<DecodedOutput> outputs;

  @Override
  public String toString() {
    return "DecodedTransaction [transactionId=" + transactionId + ", version=" + version
        + ", locktime=" + locktime + ", inputs=" + inputs + ", outputs=" + outputs + "]";
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public long getLocktime() {
    return locktime;
  }

  public void setLocktime(long locktime) {
    this.locktime = locktime;
  }

  public List<DecodedInput> getInputs() {
    return inputs;
  }

  public void setInputs(List<DecodedInput> inputs) {
    this.inputs = inputs;
  }

  public List<DecodedOutput> getOutputs() {
    return outputs;
  }

  public void setOutputs(List<DecodedOutput> outputs) {
    this.outputs = outputs;
  }

  /**
   * An object describing the signature script of input.
   */
  public static class ScriptSignature {
    /**
     * The signature script in decoded form with non-data-pushing op codes listed
     */
    @JsonProperty("asm")
    private String asm;
    /**
     * The signature script encoded as hex
     */
    @JsonProperty("hex")
    private String hex;

    @Override
    public String toString() {
      return "ScriptSignature [asm=" + asm + ", hex=" + hex + "]";
    }

    public String getAsm() {
      return asm;
    }

    public void setAsm(String asm) {
      this.asm = asm;
    }

    public String getHex() {
      return hex;
    }

    public void setHex(String hex) {
      this.hex = hex;
    }

  }

  /**
   * An object describing one of this transaction’s inputs. May be a regular input or a coinbase
   */
  public static class DecodedInput extends Outpoint {
    /**
     * An object describing the signature script of this input. Not present if this is a coinbase
     * transaction
     */
    @JsonProperty("scriptSig")
    private ScriptSignature scriptSig;
    /**
     * The coinbase (similar to the hex field of a scriptSig) encoded as hex. Only present if this
     * is a coinbase transaction
     */
    @JsonProperty("coinbase")
    private String coinbase;
    /**
     * The input sequence number
     */
    @JsonProperty("sequence")
    private long sequence;

    @Override
    public String toString() {
      return "DecodedInput [scriptSig=" + scriptSig + ", coinbase=" + coinbase + ", sequence="
          + sequence + ", getTransactionId()=" + getTransactionId() + ", getOutputIndex()="
          + getOutputIndex() + ", getAddress()=" + getAddress() + ", getAccount()=" + getAccount()
          + ", getScriptPubKey()=" + getScriptPubKey() + ", getRedeemScript()=" + getRedeemScript()
          + ", getAmount()=" + getAmount() + ", getConfirmations()=" + getConfirmations()
          + ", isSpendable()=" + isSpendable() + ", toString()=" + super.toString()
          + ", hashCode()=" + hashCode() + ", getClass()=" + getClass() + "]";
    }

    public ScriptSignature getScriptSig() {
      return scriptSig;
    }

    public void setScriptSig(ScriptSignature scriptSig) {
      this.scriptSig = scriptSig;
    }

    public String getCoinbase() {
      return coinbase;
    }

    public void setCoinbase(String coinbase) {
      this.coinbase = coinbase;
    }

    public long getSequence() {
      return sequence;
    }

    public void setSequence(long sequence) {
      this.sequence = sequence;
    }

  }

  /**
   * An object describing one of this transaction’s outputs
   */
  public static class DecodedOutput {
    /**
     * The number of bitcoins paid to this output. May be 0
     */
    @JsonProperty("value")
    private BigDecimal value;
    /**
     * The output index number of this output within this transaction
     */
    @JsonProperty("n")
    private long index;
    /**
     * An object describing the pubkey script
     */
    @JsonProperty("scriptPubKey")
    private ScriptPubKey scriptPubKey;

    @Override
    public String toString() {
      return "DecodedOutput [value=" + value + ", index=" + index + ", scriptPubKey=" + scriptPubKey
          + "]";
    }

    public BigDecimal getValue() {
      return value;
    }

    public void setValue(BigDecimal value) {
      this.value = value;
    }

    public long getIndex() {
      return index;
    }

    public void setIndex(long index) {
      this.index = index;
    }

    public ScriptPubKey getScriptPubKey() {
      return scriptPubKey;
    }

    public void setScriptPubKey(ScriptPubKey scriptPubKey) {
      this.scriptPubKey = scriptPubKey;
    }
  }

  public static class ScriptPubKey {
    /**
     * The pubkey script in decoded form with non-data-pushing op codes listed
     */
    @JsonProperty("asm")
    private String asm;
    /**
     * The pubkey script encoded as hex
     */
    @JsonProperty("hex")
    private String hex;
    /**
     * The number of signatures required; this is always 1 for P2PK, P2PKH, and P2SH (including P2SH
     * multisig because the redeem script is not available in the pubkey script). It may be greater
     * than 1 for bare multisig. This value will not be returned for nulldata or nonstandard script
     * types (see the type key below)
     */
    @JsonProperty("reqSigs")
    private long requiredSignatures;

    /**
     * The type of script
     */
    @JsonProperty("type")
    private ScriptPubKeyType scriptType;

    /**
     * The P2PKH or P2SH addresses used in this transaction, or the computed P2PKH address of any
     * pubkeys in this transaction. This array will not be returned for nulldata or nonstandard
     * script types
     */
    @JsonProperty("addresses")
    private List<String> addresses;

    @Override
    public String toString() {
      return "ScriptPubKey [asm=" + asm + ", hex=" + hex + ", requiredSignatures="
          + requiredSignatures + ", scriptType=" + scriptType + ", addresses=" + addresses + "]";
    }

    public String getAsm() {
      return asm;
    }

    public void setAsm(String asm) {
      this.asm = asm;
    }

    public String getHex() {
      return hex;
    }

    public void setHex(String hex) {
      this.hex = hex;
    }

    public long getRequiredSignatures() {
      return requiredSignatures;
    }

    public void setRequiredSignatures(long requiredSignatures) {
      this.requiredSignatures = requiredSignatures;
    }

    public ScriptPubKeyType getScriptType() {
      return scriptType;
    }

    public void setScriptType(ScriptPubKeyType scriptType) {
      this.scriptType = scriptType;
    }

    public List<String> getAddresses() {
      return addresses;
    }

    public void setAddresses(List<String> addresses) {
      this.addresses = addresses;
    }
  }
}
