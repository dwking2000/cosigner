package io.emax.cosigner.api.validation;

import io.emax.cosigner.api.core.CurrencyPackageInterface;

public interface Validator {
  /**
   * Validates a transaction.
   *
   * <p>Returns true if the transaction appears to be OK. False if cosigner should stop the
   * signing/sending process.
   */
  boolean validateTransaction(CurrencyPackageInterface currency, String transaction)
      throws Exception;
}
