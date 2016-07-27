package io.emax.cosigner.api.currency;

import java.util.Map;

/***
 * Interface that currency libraries should implement, providing common administrative information
 * and configuration access.
 */
public interface CurrencyAdmin {
  /**
   * Returns the current configuration for the currency as a key/value map.
   */
  Map<String, String> getConfiguration();

  /**
   * Provides a way to re-enable transactions that have been temporarily suspended.
   */
  void enableTransactions();

  /**
   * Provides a method to disable transactions temporarily.
   */
  void disableTransactions();

  /**
   * Informs caller of the transaction state.
   */
  boolean transactionsEnabled();
}
