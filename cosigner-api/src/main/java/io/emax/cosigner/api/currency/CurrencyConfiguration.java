package io.emax.cosigner.api.currency;

/**
 * Common configuration that each currency needs to provide to cosigner-core.
 * 
 * @author Tom
 *
 */
public interface CurrencyConfiguration {
  /**
   * Currency symbol.
   */
  String getCurrencySymbol();

  /**
   * Experimental placeholder Will return what kind of signatures the currency needs. - Async
   * multi-sig (ETH) - Everyone signs before submitting (BTC) - 2-phase if that's different
   */
  SigningType getSigningType();

  int getMinSignatures();
}
