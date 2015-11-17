package io.emax.cosigner.api.validation;

import io.emax.cosigner.api.core.CurrencyPackage;

public interface Validator {
  /**
   * Validates a transaction.
   * 
   * <p>Returns true if the transaction appears to be OK. False if cosigner should stop the
   * signing/sending process.
   */
  boolean validateTransaction(CurrencyPackage currency, String transaction);
}
