package io.emax.heimdal.api.currency;

import java.util.Map;

import rx.Observable;

/**
 * Continuously polls for account balances at an acceptable rate for the currency
 * 
 * @author Tom
 *
 */
public interface Monitor {

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
}
