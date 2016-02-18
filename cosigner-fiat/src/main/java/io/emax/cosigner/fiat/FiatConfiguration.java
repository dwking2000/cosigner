package io.emax.cosigner.fiat;

import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.SigningType;

public class FiatConfiguration implements CurrencyConfiguration {
  @Override
  public String getCurrencySymbol() {
    return null;
  }

  @Override
  public SigningType getSigningType() {
    return null;
  }

  @Override
  public int getMinSignatures() {
    return 0;
  }

  @Override
  public boolean hasMultipleSenders() {
    return false;
  }

  @Override
  public boolean hasMultipleRecipients() {
    return false;
  }
}
