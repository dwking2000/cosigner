package io.emax.heimdal.bitcoin.bitcoindrpc;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author dquintela
 */
public class AddressReceived {
  /**
   * Added in Bitcoin Core 0.10.0
   * 
   * Set to true if this address is a watch-only address which has received a spendable payment
   * (that is, a payment with at least the specified number of confirmations and which is not an
   * immature coinbase). Otherwise not returned
   */
  @JsonProperty("involvesWatchonly")
  private boolean involvesWatchonly;
  /**
   * The address being described encoded in base58check
   */
  @JsonProperty("address")
  private String address;

  /**
   * The account the address belongs to; may be the default account, an empty string (""â€�)
   */
  @JsonProperty("account")
  private String account;

  /**
   * The total amount the address has received in bitcoins
   */
  @JsonProperty("amount")
  private BigDecimal amount;

  /**
   * The number of confirmations of the latest transaction to the address. May be 0 for unconfirmed
   */
  @JsonProperty("confirmations")
  private long confirmations;

  /**
   * An array of TXIDs belonging to transactions that pay the address
   */
  @JsonProperty("txids")
  private List<String> transactionIds;

  @Override
  public String toString() {
    return "AddressReceived [involvesWatchonly=" + involvesWatchonly + ", address=" + address
        + ", account=" + account + ", amount=" + amount + ", confirmations=" + confirmations
        + ", transactionIds=" + transactionIds + "]";
  }

  public boolean isInvolvesWatchonly() {
    return involvesWatchonly;
  }

  public void setInvolvesWatchonly(boolean involvesWatchonly) {
    this.involvesWatchonly = involvesWatchonly;
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

  public List<String> getTransactionIds() {
    return transactionIds;
  }

  public void setTransactionIds(List<String> transactionIds) {
    this.transactionIds = transactionIds;
  }
}
