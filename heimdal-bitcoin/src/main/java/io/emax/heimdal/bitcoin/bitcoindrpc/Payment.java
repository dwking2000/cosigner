package io.emax.heimdal.bitcoin.bitcoindrpc;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A payment or internal accounting entry
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
    orphan, /** if an off-block-chain move made with the move RPC */
    move
  }

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
   * For an output, the output index (vout) for this output in this transaction. For an input, the
   * output index for the output being spent in its transaction. Because inputs list the output
   * indexes from previous transactions, more than one entry in the details array may have the same
   * output index. Not returned for move category payments
   */
  @JsonProperty("vout")
  private int vout;

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
  private String blockhash;

  /**
   * Only returned for confirmed transactions. The block height of the block on the local best block
   * chain which includes this transaction
   */
  @JsonProperty("blockindex")
  private Long blockindex;

  /**
   * Only returned for confirmed transactions. The block header time (Unix epoch time) of the block
   * on the local best block chain which includes this transaction
   */
  @JsonProperty("blocktime")
  private Date blocktime;

  /**
   * The TXID of the transaction, encoded as hex in RPC byte order. Not returned for move category
   * payments
   */
  @JsonProperty("txid")
  private String txid;

  /**
   * An array containing the TXIDs of other transactions that spend the same inputs (UTXOs) as this
   * transaction. Array may be empty
   * 
   * The TXID of a conflicting transaction, encoded as hex in RPC byte order
   */
  @JsonProperty("walletconflicts")
  private List<String> walletconflicts;

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
  private Date timereceived;

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

  /**
   * Only returned by move category payments. This is the account the bitcoins were moved from or
   * moved to, as indicated by a negative or positive amount field in this payment
   */
  @JsonProperty("otheraccount")
  private String otheraccount;

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public PaymentCategory getCategory() {
    return category;
  }

  public void setCategory(PaymentCategory category) {
    this.category = category;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public int getVout() {
    return vout;
  }

  public void setVout(int vout) {
    this.vout = vout;
  }

  public BigDecimal getFee() {
    return fee;
  }

  public void setFee(BigDecimal fee) {
    this.fee = fee;
  }

  public long getConfirmations() {
    return confirmations;
  }

  public void setConfirmations(long confirmations) {
    this.confirmations = confirmations;
  }

  public boolean isGenerated() {
    return generated;
  }

  public void setGenerated(boolean generated) {
    this.generated = generated;
  }

  public String getBlockhash() {
    return blockhash;
  }

  public void setBlockhash(String blockhash) {
    this.blockhash = blockhash;
  }

  public Long getBlockindex() {
    return blockindex;
  }

  public void setBlockindex(Long blockindex) {
    this.blockindex = blockindex;
  }

  public Date getBlocktime() {
    return blocktime;
  }

  public void setBlocktime(Date blocktime) {
    this.blocktime = blocktime;
  }

  public String getTxid() {
    return txid;
  }

  public void setTxid(String txid) {
    this.txid = txid;
  }

  public List<String> getWalletconflicts() {
    return walletconflicts;
  }

  public void setWalletconflicts(List<String> walletconflicts) {
    this.walletconflicts = walletconflicts;
  }

  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }

  public Date getTimereceived() {
    return timereceived;
  }

  public void setTimereceived(Date timereceived) {
    this.timereceived = timereceived;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getOtheraccount() {
    return otheraccount;
  }

  public void setOtheraccount(String otheraccount) {
    this.otheraccount = otheraccount;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((account == null) ? 0 : account.hashCode());
    result = prime * result + ((address == null) ? 0 : address.hashCode());
    result = prime * result + ((amount == null) ? 0 : amount.hashCode());
    result = prime * result + ((blockhash == null) ? 0 : blockhash.hashCode());
    result = prime * result + ((blockindex == null) ? 0 : blockindex.hashCode());
    result = prime * result + ((blocktime == null) ? 0 : blocktime.hashCode());
    result = prime * result + ((category == null) ? 0 : category.hashCode());
    result = prime * result + ((comment == null) ? 0 : comment.hashCode());
    result = prime * result + (int) (confirmations ^ (confirmations >>> 32));
    result = prime * result + ((fee == null) ? 0 : fee.hashCode());
    result = prime * result + (generated ? 1231 : 1237);
    result = prime * result + ((otheraccount == null) ? 0 : otheraccount.hashCode());
    result = prime * result + ((time == null) ? 0 : time.hashCode());
    result = prime * result + ((timereceived == null) ? 0 : timereceived.hashCode());
    result = prime * result + ((to == null) ? 0 : to.hashCode());
    result = prime * result + ((txid == null) ? 0 : txid.hashCode());
    result = prime * result + vout;
    result = prime * result + ((walletconflicts == null) ? 0 : walletconflicts.hashCode());
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
    Payment other = (Payment) obj;
    if (account == null) {
      if (other.account != null)
        return false;
    } else if (!account.equals(other.account))
      return false;
    if (address == null) {
      if (other.address != null)
        return false;
    } else if (!address.equals(other.address))
      return false;
    if (amount == null) {
      if (other.amount != null)
        return false;
    } else if (!amount.equals(other.amount))
      return false;
    if (blockhash == null) {
      if (other.blockhash != null)
        return false;
    } else if (!blockhash.equals(other.blockhash))
      return false;
    if (blockindex == null) {
      if (other.blockindex != null)
        return false;
    } else if (!blockindex.equals(other.blockindex))
      return false;
    if (blocktime == null) {
      if (other.blocktime != null)
        return false;
    } else if (!blocktime.equals(other.blocktime))
      return false;
    if (category != other.category)
      return false;
    if (comment == null) {
      if (other.comment != null)
        return false;
    } else if (!comment.equals(other.comment))
      return false;
    if (confirmations != other.confirmations)
      return false;
    if (fee == null) {
      if (other.fee != null)
        return false;
    } else if (!fee.equals(other.fee))
      return false;
    if (generated != other.generated)
      return false;
    if (otheraccount == null) {
      if (other.otheraccount != null)
        return false;
    } else if (!otheraccount.equals(other.otheraccount))
      return false;
    if (time == null) {
      if (other.time != null)
        return false;
    } else if (!time.equals(other.time))
      return false;
    if (timereceived == null) {
      if (other.timereceived != null)
        return false;
    } else if (!timereceived.equals(other.timereceived))
      return false;
    if (to == null) {
      if (other.to != null)
        return false;
    } else if (!to.equals(other.to))
      return false;
    if (txid == null) {
      if (other.txid != null)
        return false;
    } else if (!txid.equals(other.txid))
      return false;
    if (vout != other.vout)
      return false;
    if (walletconflicts == null) {
      if (other.walletconflicts != null)
        return false;
    } else if (!walletconflicts.equals(other.walletconflicts))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Payment [account=" + account + ", address=" + address + ", category=" + category
        + ", amount=" + amount + ", vout=" + vout + ", fee=" + fee + ", confirmations="
        + confirmations + ", generated=" + generated + ", blockhash=" + blockhash + ", blockindex="
        + blockindex + ", blocktime=" + blocktime + ", txid=" + txid + ", walletconflicts="
        + walletconflicts + ", time=" + time + ", timereceived=" + timereceived + ", comment="
        + comment + ", to=" + to + ", otheraccount=" + otheraccount + "]";
  }
}
