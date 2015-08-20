package io.emax.heimdal.api.currency;

/**
 * Common configuration that each currency needs to provide to heimdal-core
 * 
 * @author Tom
 *
 */
public interface CurrencyConfiguration {
  /**
   * Currency symbol
   * 
   * @return
   */
  String getCurrencySymbol();

  /**
   * Experimental placeholder Will return what kind of signatures the currency needs. - Async
   * multi-sig (ETH) - Everyone signs before submitting (BTC) - 2-phase if that's different
   */
  SigningType getSigningType();

  int getMinSignatures();
}
