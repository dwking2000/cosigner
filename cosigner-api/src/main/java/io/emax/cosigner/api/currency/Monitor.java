package io.emax.cosigner.api.currency;

import io.emax.cosigner.api.currency.Wallet.TransactionDetails;

import rx.Observable;

import java.util.Map;
import java.util.Set;

/**
 * Continuously polls for account balances at an acceptable rate for the currency.
 *
 * @author Tom
 */
public interface Monitor {

  /**
   * Create a new monitor using the same RPC and wallet settings as the current one.
   */
  Monitor createNewMonitor();

  /**
   * Destroys a monitor when we're done with it.
   */
  void destroyMonitor();

  /**
   * Add addresses to the monitored list.
   */
  void addAddresses(Iterable<String> addresses);

  /**
   * Remove addresses from the monitored list.
   */
  void removeAddresses(Iterable<String> addresses);

  /**
   * Get a list of monitored addresses.
   */
  Iterable<String> listAddresses();

  /**
   * Get the most up to date list of balances for any monitored accounts.
   *
   * @return Map of (Address, Balance)
   */
  Map<String, String> getBalances();

  /**
   * Subscribe-able version of getBalances.
   */
  Observable<Map<String, String>> getObservableBalances();

  /**
   * Get any unread transactions for the monitored addresses.
   */
  Set<TransactionDetails> getTransactions();

  /**
   * Subscribe-able version of getTransactions.
   */
  Observable<Set<TransactionDetails>> getObservableTransactions();
}
