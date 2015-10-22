package io.emax.cosigner.api.currency;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Wallet interface
 * 
 * @author Tom
 *
 */
public interface Wallet {
  /**
   * Returns a new regular address. It's assumed the wallet will provide a way to backup the private
   * keys
   * 
   * @param name Name associated to the account, for deterministic addresses.
   * @return Public key/address of the account.
   */
  String createAddress(String name);

  /**
   * Returns all addresses stored for the provided wallet name, they should be deterministic.
   * 
   * @param name
   * @return
   */
  Iterable<String> getAddresses(String name);

  /**
   * Provides a multi-sig account for the given addresses. There may be additional addresses
   * involved if provided in the currency configuration. The order of the addresses may matter
   * depending on the currency.
   * 
   * @param addresses
   * @param name Associate the account with this user key
   * @return
   */
  String getMultiSigAddress(Iterable<String> addresses, String name);

  /**
   * Returns a balance for the given account
   * 
   * @param address
   * @return
   */
  String getBalance(String address);

  public class Recipient {
    private String recipientAddress;
    private BigDecimal amount;

    public String getRecipientAddress() {
      return recipientAddress;
    }

    public void setRecipientAddress(String recipientAddress) {
      this.recipientAddress = recipientAddress;
    }

    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(BigDecimal amount) {
      this.amount = amount;
    }
  }

  /**
   * Create an unsigned transaction transferring funds between the provided accounts.
   * 
   * @param fromAddress
   * @param toAddress
   * @param amount
   * @return Unsigned transaction
   */
  String createTransaction(Iterable<String> fromAddresses, Iterable<Recipient> toAddresses);

  /**
   * Sign the provided transaction with the provided address' private keys if available Any existing
   * signing validation should be called before calling this interface I.E. Amount range,
   * transaction counts for the day, untrusted recipient, etc...
   * 
   * @param transaction
   * @param address
   * @return Same transaction with new signature data
   */
  String signTransaction(String transaction, String address);

  /**
   * Sign the provided transaction, the address' private key will be generated with the provided
   * account name.
   * 
   * @param transaction
   * @param address
   * @param name
   * @return Same transaction with new signature data
   */
  String signTransaction(String transaction, String address, String name);

  /**
   * Submits the provided transaction to the network
   * 
   * @param transaction
   * @return Transaction identifier to allow for tracking
   */
  String sendTransaction(String transaction);

  public class TransactionDetails {
    private String txHash;
    private String[] fromAddress;
    private String[] toAddress;
    private BigDecimal amount;

    public String getTxHash() {
      return txHash;
    }

    public void setTxHash(String txHash) {
      this.txHash = txHash;
    }

    public String[] getFromAddress() {
      return fromAddress.clone();
    }

    public void setFromAddress(String[] fromAddress) {
      this.fromAddress = fromAddress.clone();
    }

    public String[] getToAddress() {
      return toAddress.clone();
    }

    public void setToAddress(String[] toAddress) {
      this.toAddress = toAddress.clone();
    }

    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(BigDecimal amount) {
      this.amount = amount;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((amount == null) ? 0 : amount.hashCode());
      result = prime * result + Arrays.hashCode(fromAddress);
      result = prime * result + Arrays.hashCode(toAddress);
      result = prime * result + ((txHash == null) ? 0 : txHash.hashCode());
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
      TransactionDetails other = (TransactionDetails) obj;
      if (amount == null) {
        if (other.amount != null)
          return false;
      } else if (!amount.equals(other.amount))
        return false;
      if (!Arrays.equals(fromAddress, other.fromAddress))
        return false;
      if (!Arrays.equals(toAddress, other.toAddress))
        return false;
      if (txHash == null) {
        if (other.txHash != null)
          return false;
      } else if (!txHash.equals(other.txHash))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "TransactionDetails [txHash=" + txHash + ", fromAddress="
          + Arrays.toString(fromAddress) + ", toAddress=" + Arrays.toString(toAddress) + ", amount="
          + amount + "]";
    }
  }

  /**
   * List transactions from a given address
   * 
   * @param address address to lookup transactions for.
   * @param numberToReturn Number of transactions to return.
   * @param skipNumber Skip the first skipNumber results for pagination.
   * @return Transaction Details
   */
  TransactionDetails[] getTransactions(String address, int numberToReturn, int skipNumber);
}
