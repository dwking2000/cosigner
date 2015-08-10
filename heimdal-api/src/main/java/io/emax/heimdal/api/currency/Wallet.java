package io.emax.heimdal.api.currency;

import java.math.BigDecimal;

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

  /**
   * Create an unsigned transaction transferring funds between the provided accounts.
   * 
   * @param fromAddress
   * @param toAddress
   * @param amount
   * @return Unsigned transaction
   */
  String createTransaction(Iterable<String> fromAddress, String toAddress, BigDecimal amount);

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
}
