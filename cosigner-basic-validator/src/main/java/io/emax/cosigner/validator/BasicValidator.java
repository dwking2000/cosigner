package io.emax.cosigner.validator;

import io.emax.cosigner.api.core.CurrencyPackage;
import io.emax.cosigner.api.currency.Wallet;
import io.emax.cosigner.api.currency.Wallet.TransactionDetails;
import io.emax.cosigner.api.validation.Validatable;
import io.emax.cosigner.api.validation.Validator;
import io.emax.cosigner.api.validation.ValidatorConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class BasicValidator implements Validator {
  private static final Logger LOGGER = LoggerFactory.getLogger(BasicValidator.class);

  @Override
  public boolean validateTransaction(CurrencyPackage currency, String transaction) {
    if (!Validatable.class.isAssignableFrom(currency.getWallet().getClass())) {
      // Wallet can not be validated.
      LOGGER.debug("Tried validating: " + currency.getConfiguration().getCurrencySymbol()
          + ", was not Validatable.");
      return true;
    }

    if (!ValidatorConfiguration.class.isAssignableFrom(currency.getConfiguration().getClass())) {
      // Configuration is not set up, we can't validate.
      LOGGER.debug("Tried validating: " + currency.getConfiguration().getCurrencySymbol()
          + ", was not Configured.");
      return true;
    }

    LOGGER.debug("Attempting to validate: " + currency.getConfiguration().getCurrencySymbol());
    Wallet wallet = currency.getWallet();
    Validatable validatableWallet = (Validatable) currency.getWallet();
    ValidatorConfiguration validatorConfig = (ValidatorConfiguration) currency.getConfiguration();

    TransactionDetails txDetail = validatableWallet.decodeRawTransaction(transaction);
    if (validatorConfig.getMaxAmountPerTransaction().compareTo(BigDecimal.ZERO) != 0
        && txDetail.getAmount().compareTo(validatorConfig.getMaxAmountPerTransaction()) > 0) {
      LOGGER.info("Transaction value too high for: " + txDetail.toString());
      return false;
    }

    // Build timed totals.
    BigDecimal hourlyTotal = BigDecimal.ZERO;
    BigDecimal dailyTotal = BigDecimal.ZERO;
    Instant oneHourAgo = Clock.systemUTC().instant().minus(1, ChronoUnit.HOURS);
    Instant oneDayAgo = Clock.systemUTC().instant().minus(1, ChronoUnit.DAYS);
    for (String senders : txDetail.getFromAddress()) {
      TransactionDetails[] txs = wallet.getTransactions(senders, 100, 0);
      for (TransactionDetails tx : txs) {
        if (tx.getTxDate().toInstant().isAfter(oneHourAgo)) {
          hourlyTotal = hourlyTotal.add(tx.getAmount());
        }
        if (tx.getTxDate().toInstant().isAfter(oneDayAgo)) {
          dailyTotal = dailyTotal.add(tx.getAmount());
        }
      }
    }

    // Verify that the tx + timed totals are within bounds.
    BigDecimal maxPerHour = validatorConfig.getMaxAmountPerHour();
    hourlyTotal = hourlyTotal.add(txDetail.getAmount());
    if (maxPerHour.compareTo(BigDecimal.ZERO) != 0 && hourlyTotal.compareTo(maxPerHour) > 0) {
      LOGGER.info("Transaction value exceeds hourly limit.");
      return false;
    }
    if (validatorConfig.getMaxAmountPerDay().compareTo(BigDecimal.ZERO) != 0 && dailyTotal
        .add(txDetail.getAmount()).compareTo(validatorConfig.getMaxAmountPerDay()) > 0) {
      LOGGER.info("Transaction value exceeds daily limit.");
      return false;
    }

    // Nothing failed, so it's ok.
    LOGGER.debug("Validation passed");
    return true;
  }

}
