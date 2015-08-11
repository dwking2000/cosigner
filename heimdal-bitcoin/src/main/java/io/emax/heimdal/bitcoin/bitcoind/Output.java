package io.emax.heimdal.bitcoin.bitcoind;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author dquintela
 */
public class Output extends Outpoint {
  /**
   * The P2PKH or P2SH address the output paid. Only returned for P2PKH or P2SH output scripts
   */
  @JsonProperty("address")
  private String address;

  /**
   * If the address returned belongs to an account, this is the account. Otherwise not returned
   */
  @JsonProperty("account")
  private String account;

  /**
   * The output script paid, encoded as hex
   */
  @JsonProperty("scriptPubKey")
  private String scriptPubKey;

  /**
   * If the output is a P2SH whose script belongs to this wallet, this is the redeem script
   */
  @JsonProperty("redeemScript")
  private String redeemScript;

  /**
   * The amount paid to the output in bitcoins
   */
  @JsonProperty("amount")
  private BigDecimal amount;

  /**
   * The number of confirmations received for the transaction containing this output
   */
  @JsonProperty("confirmations")
  private long confirmations;

  /**
   * Added in Bitcoin Core 0.10.0 Set to true if the private key or keys needed to spend this output
   * are part of the wallet. Set to false if not (such as for watch-only addresses)
   */
  @JsonProperty("spendable")
  private boolean spendable;

  @Override
  public String toString() {
    return "Output [address=" + address + ", account=" + account + ", scriptPubKey=" + scriptPubKey
        + ", redeemScript=" + redeemScript + ", amount=" + amount + ", confirmations="
        + confirmations + ", spendable=" + spendable + "]";
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public String getScriptPubKey() {
    return scriptPubKey;
  }

  public void setScriptPubKey(String scriptPubKey) {
    this.scriptPubKey = scriptPubKey;
  }

  public String getRedeemScript() {
    return redeemScript;
  }

  public void setRedeemScript(String redeemScript) {
    this.redeemScript = redeemScript;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public long getConfirmations() {
    return confirmations;
  }

  public void setConfirmations(long confirmations) {
    this.confirmations = confirmations;
  }

  public boolean isSpendable() {
    return spendable;
  }

  public void setSpendable(boolean spendable) {
    this.spendable = spendable;
  }
}