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
import java.util.Date;

public class BasicValidator implements Validator {
  private static final Logger logger = LoggerFactory.getLogger(BasicValidator.class);

  @Override
  public boolean validateTransaction(CurrencyPackage currency, String transaction) {
    if (!Validatable.class.isAssignableFrom(currency.getWallet().getClass())) {
      // Wallet can not be validated.
      logger.debug("Tried validating: " + currency.getConfiguration().getCurrencySymbol()
          + ", was not Validatable.");
      return true;
    }

    if (!ValidatorConfiguration.class.isAssignableFrom(currency.getConfiguration().getClass())) {
      // Configuration is not set up, we can't validate.
      logger.debug("Tried validating: " + currency.getConfiguration().getCurrencySymbol()
          + ", was not Configured.");
      return true;
    }

    logger.debug("Attempting to validate: " + currency.getConfiguration().getCurrencySymbol());
    Wallet wallet = currency.getWallet();
    Validatable validatableWallet = (Validatable) currency.getWallet();
    ValidatorConfiguration validatorConfig = (ValidatorConfiguration) currency.getConfiguration();

    TransactionDetails txDetail = validatableWallet.decodeRawTransaction(transaction);
    if (validatorConfig.getMaxAmountPerTransaction().compareTo(BigDecimal.ZERO) != 0
        && txDetail.getAmount().compareTo(validatorConfig.getMaxAmountPerTransaction()) > 0) {
      logger.info("Transaction value too high for: " + txDetail.toString());
      return false;
    }

    BigDecimal hourlyTotal = BigDecimal.ZERO;
    BigDecimal dailyTotal = BigDecimal.ZERO;
    Date oneHourAgo = new Date(System.currentTimeMillis() - (1 * 60 * 60 * 1000));
    Date oneDayAgo = new Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000));
    for (String senders : txDetail.getFromAddress()) {
      TransactionDetails[] txs = wallet.getTransactions(senders, 100, 0);
      for (TransactionDetails tx : txs) {
        if (tx.getTxDate().after(oneHourAgo)) {
          hourlyTotal = hourlyTotal.add(tx.getAmount());
        }
        if (tx.getTxDate().after(oneDayAgo)) {
          dailyTotal = dailyTotal.add(tx.getAmount());
        }
      }
    }

    // Verify that the tx + timed totals are within bounds.
    if (validatorConfig.getMaxAmountPerHour().compareTo(BigDecimal.ZERO) != 0 && hourlyTotal
        .add(txDetail.getAmount()).compareTo(validatorConfig.getMaxAmountPerHour()) > 0) {
      logger.info("Transaction value exceeds hourly limit.");
      return false;
    }
    if (validatorConfig.getMaxAmountPerDay().compareTo(BigDecimal.ZERO) != 0 && dailyTotal
        .add(txDetail.getAmount()).compareTo(validatorConfig.getMaxAmountPerDay()) > 0) {
      logger.info("Transaction value exceeds daily limit.");
      return false;
    }

    // Nothing failed, so it's ok.
    logger.debug("Validation passed");
    return true;
  }

}
