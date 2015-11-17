package io.emax.cosigner.api.validation;

import io.emax.cosigner.api.currency.Wallet.TransactionDetails;

/**
 * Indicates whether a currency's wallet makes sense to be validated.
 */
public interface Validatable {
  /**
   * Decode a transaction returned by the prepare and/or sign step.
   */
  TransactionDetails decodeRawTransaction(String transaction);
}
