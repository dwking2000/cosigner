package io.emax.heimdal.api.currency;

import java.util.Map;
import java.util.Set;

import io.emax.heimdal.api.currency.Wallet.TransactionDetails;
import rx.Observable;

/**
 * Continuously polls for account balances at an acceptable rate for the currency
 * 
 * @author Tom
 *
 */
public interface Monitor {
  
  /**
   * Create a new monitor using the same RPC and wallet settings as the current one.
   * @return
   */
  Monitor createNewMonitor();
  
  /**
   * Destroys a monitor when we're done with it
   */
  void destroyMonitor();

  /**
   * Add addresses to the monitored list
   * 
   * @param addresses
   */
  void addAddresses(Iterable<String> addresses);

  /**
   * Remove addresses from the monitored list
   * 
   * @param addresses
   */
  void removeAddresses(Iterable<String> addresses);

  /**
   * Get a list of monitored addresses
   * 
   * @return
   */
  Iterable<String> listAddresses();

  /**
   * Get the most up to date list of balances for any monitored accounts
   * 
   * @return
   */
  Map<String, String> getBalances();

  /**
   * Subscribe-able version of getBalances
   * 
   * @return
   */
  Observable<Map<String, String>> getObservableBalances();
  
  /**
   * 
   * @return
   */
  Set<TransactionDetails> getTransactions();
  
  /**
   * 
   * @return
   */
  Observable<Set<TransactionDetails>> getObservableTransactions();
}
