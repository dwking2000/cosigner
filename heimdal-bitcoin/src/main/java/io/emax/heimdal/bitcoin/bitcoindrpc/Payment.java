package io.emax.heimdal.bitcoin.bitcoindrpc;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * An payment which did not appear in the specified block or an earlier block
 * 
 * @author dquintela
 */
public class Payment {
  public enum PaymentCategory {
    /** if sending payment */
    send, /** if this wallet received payment in a regular transaction */
    receive, /** if a matured and spendable coinbase */
    generate, /** if a coinbase that is not spendable yet */
    immature, /** if a coinbase from a block that’s not in the local best block chain */
    orphan
  }

  /**
   * Added in Bitcoin Core 0.10.0
   * 
   * Set to true if the payment involves a watch-only address. Otherwise not returned
   */
  @JsonProperty("involvesWatchonly")
  private boolean watchOnlyAddress;

  /**
   * The account which the payment was credited to or debited from. May be an empty string ("") for
   * the default account
   */
  @JsonProperty("account")
  private String account;

  /**
   * The address paid in this payment, which may be someone else’s address not belonging to this
   * wallet. May be empty if the address is unknown, such as when paying to a non-standard pubkey
   * script
   */
  @JsonProperty("address")
  private String address;

  /**
   * The category of payment/transaction
   */
  @JsonProperty("category")
  private PaymentCategory category;

  /**
   * A negative bitcoin amount if sending payment; a positive bitcoin amount if receiving payment
   * (including coinbases)
   */
  @JsonProperty("amount")
  private BigDecimal amount;

  /**
   * txid, The TXID of the transaction, encoded as hex in RPC byte order
   * 
   * vout, Added in Bitcoin Core 0.10.0 For an output, the output index (vout) for this output in
   * this transaction. For an input, the output index for the output being spent in its transaction.
   * Because inputs list the output indexes from previous transactions, more than one entry in the
   * details array may have the same output index
   */
  @JsonUnwrapped
  private Outpoint output;

  /**
   * If sending payment, the fee paid as a negative bitcoins value. May be 0. Not returned if
   * receiving payment
   */
  @JsonProperty("fee")
  private BigDecimal fee;

  /**
   * The number of confirmations the transaction has received. Will be 0 for unconfirmed and -1 for
   * conflicted
   */
  @JsonProperty("confirmations")
  private long confirmations;

  /**
   * Set to true if the transaction is a coinbase. Not returned for regular transactions
   */
  @JsonProperty("generated")
  private boolean generated;

  /**
   * Only returned for confirmed transactions. The hash of the block on the local best block chain
   * which includes this transaction, encoded as hex in RPC byte order
   */
  @JsonProperty("blockhash")
  private String blockHash;

  /**
   * Only returned for confirmed transactions. The block height of the block on the local best block
   * chain which includes this transaction
   */
  @JsonProperty("blockindex")
  private Long blockHeight;

  /**
   * Only returned for confirmed transactions. The block header time (Unix epoch time) of the block
   * on the local best block chain which includes this transaction
   */
  @JsonProperty("blocktime")
  private Date blockTime;

  /**
   * An array containing the TXIDs of other transactions that spend the same inputs (UTXOs) as this
   * transaction. Array may be empty
   * 
   * The TXID of a conflicting transaction, encoded as hex in RPC byte order
   */
  @JsonProperty("walletconflicts")
  private List<String> conflictingTransactionIds;

  /**
   * A Unix epoch time when the transaction was added to the wallet
   */
  @JsonProperty("time")
  private Date time;

  /**
   * A Unix epoch time when the transaction was detected by the local node, or the time of the block
   * on the local best block chain that included the transaction
   */
  @JsonProperty("timereceived")
  private Date received;

  /**
   * For transaction originating with this wallet, a locally-stored comment added to the
   * transaction. Only returned if a comment was added
   */
  @JsonProperty("comment")
  private String comment;

  /**
   * For transaction originating with this wallet, a locally-stored comment added to the transaction
   * identifying who the transaction was sent to. Only returned if a comment-to was added
   */
  @JsonProperty("to")
  private String to;

  @Override
  public String toString() {
    return "Payment [watchOnlyAddress=" + watchOnlyAddress + ", account=" + account + ", address="
        + address + ", category=" + category + ", amount=" + amount + ", output=" + output
        + ", fee=" + fee + ", confirmations=" + confirmations + ", generated=" + generated
        + ", blockHash=" + blockHash + ", blockHeight=" + blockHeight + ", blockTime=" + blockTime
        + ", conflictingTransactionIds=" + conflictingTransactionIds + ", time=" + time
        + ", received=" + received + ", comment=" + comment + ", to=" + to + "]";
  }

  /**
   * @return the watchOnlyAddress
   */
  public boolean isWatchOnlyAddress() {
    return watchOnlyAddress;
  }

  /**
   * @param watchOnlyAddress the watchOnlyAddress to set
   */
  public void setWatchOnlyAddress(boolean watchOnlyAddress) {
    this.watchOnlyAddress = watchOnlyAddress;
  }

  /**
   * @return the account
   */
  public String getAccount() {
    return account;
  }

  /**
   * @param account the account to set
   */
  public void setAccount(String account) {
    this.account = account;
  }

  /**
   * @return the address
   */
  public String getAddress() {
    return address;
  }

  /**
   * @param address the address to set
   */
  public void setAddress(String address) {
    this.address = address;
  }

  /**
   * @return the category
   */
  public PaymentCategory getCategory() {
    return category;
  }

  /**
   * @param category the category to set
   */
  public void setCategory(PaymentCategory category) {
    this.category = category;
  }

  /**
   * @return the amount
   */
  public BigDecimal getAmount() {
    return amount;
  }

  /**
   * @param amount the amount to set
   */
  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  /**
   * @return the output
   */
  public Outpoint getOutput() {
    return output;
  }

  /**
   * @param output the output to set
   */
  public void setOutput(Outpoint output) {
    this.output = output;
  }

  /**
   * @return the fee
   */
  public BigDecimal getFee() {
    return fee;
  }

  /**
   * @param fee the fee to set
   */
  public void setFee(BigDecimal fee) {
    this.fee = fee;
  }

  /**
   * @return the confirmations
   */
  public long getConfirmations() {
    return confirmations;
  }

  /**
   * @param confirmations the confirmations to set
   */
  public void setConfirmations(long confirmations) {
    this.confirmations = confirmations;
  }

  /**
   * @return the generated
   */
  public boolean isGenerated() {
    return generated;
  }

  /**
   * @param generated the generated to set
   */
  public void setGenerated(boolean generated) {
    this.generated = generated;
  }

  /**
   * @return the blockHash
   */
  public String getBlockHash() {
    return blockHash;
  }

  /**
   * @param blockHash the blockHash to set
   */
  public void setBlockHash(String blockHash) {
    this.blockHash = blockHash;
  }

  /**
   * @return the blockHeight
   */
  public Long getBlockHeight() {
    return blockHeight;
  }

  /**
   * @param blockHeight the blockHeight to set
   */
  public void setBlockHeight(Long blockHeight) {
    this.blockHeight = blockHeight;
  }

  /**
   * @return the blockTime
   */
  public Date getBlockTime() {
    return blockTime;
  }

  /**
   * @param blockTime the blockTime to set
   */
  public void setBlockTime(Date blockTime) {
    this.blockTime = blockTime;
  }

  /**
   * @return the conflictingTransactionIds
   */
  public List<String> getConflictingTransactionIds() {
    return conflictingTransactionIds;
  }

  /**
   * @param conflictingTransactionIds the conflictingTransactionIds to set
   */
  public void setConflictingTransactionIds(List<String> conflictingTransactionIds) {
    this.conflictingTransactionIds = conflictingTransactionIds;
  }

  /**
   * @return the time
   */
  public Date getTime() {
    return time;
  }

  /**
   * @param time the time to set
   */
  public void setTime(Date time) {
    this.time = time;
  }

  /**
   * @return the received
   */
  public Date getReceived() {
    return received;
  }

  /**
   * @param received the received to set
   */
  public void setReceived(Date received) {
    this.received = received;
  }

  /**
   * @return the comment
   */
  public String getComment() {
    return comment;
  }

  /**
   * @param comment the comment to set
   */
  public void setComment(String comment) {
    this.comment = comment;
  }

  /**
   * @return the to
   */
  public String getTo() {
    return to;
  }

  /**
   * @param to the to to set
   */
  public void setTo(String to) {
    this.to = to;
  }
}
