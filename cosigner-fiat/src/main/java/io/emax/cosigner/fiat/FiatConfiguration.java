package io.emax.cosigner.fiat;

import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.SigningType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;

public class FiatConfiguration implements CurrencyConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(FiatConfiguration.class);

  private static String currencySymbol = "FIAT";
  private static String serverPrivateKey = "deadbeef";
  private static int minSignatures = 10;
  private static int minConfirmations = 10;
  private static long gasPrice = 100000000000L;
  private static long contractGas = 3000000L;
  private static String contractAccount = "4839540a0ae3242fadf288622f7de1a9278a5858";
  private static String adminAccount = "4839540a0ae3242fadf288622f7de1a9278a5858";
  private static String[] multiSigAccounts = {"4839540a0ae3242fadf288622f7de1a9278a5858"};
  private static boolean generateNewContract = true;
  private static String contractAddress = "";
  private static BigDecimal maxAmountPerHour = BigDecimal.ZERO;
  private static BigDecimal maxAmountPerDay = BigDecimal.ZERO;
  private static BigDecimal maxAmountPerTransaction = BigDecimal.ZERO;

  private static boolean configLoaded = false;

  public FiatConfiguration(String currency) {
    loadConfig(currency);
  }

  /*private static long getLongProp(Properties prop, String value, long defaultValue) {
    try {
      return Long.parseLong(prop.getProperty(value));
    } catch (Exception e) {
      LOGGER.warn(null, e);
      return defaultValue;
    }
  }*/

  private synchronized void loadConfig(String currency) {
    if (!configLoaded) {
      configLoaded = true;

      FileInputStream propertiesFile = null;
      String propertiesFilePath = "./cosigner-" + currency + ".properties";

      try {
        Properties cosignerProperties = new Properties();
        propertiesFile = new FileInputStream(propertiesFilePath);

        cosignerProperties.load(propertiesFile);
        propertiesFile.close();

        // TODO Load properties file.
      } catch (IOException e) {
        if (propertiesFile != null) {
          try {
            propertiesFile.close();
          } catch (IOException e1) {
            LOGGER.warn(null, e1);
          }
        }
        LOGGER.debug(null, e);
        LOGGER.info("Could not load cosigner-fiat configuration, using defaults.");
      }
    }
  }

  @Override
  public String getCurrencySymbol() {
    return currencySymbol;
  }

  @Override
  public SigningType getSigningType() {
    return SigningType.SENDALL;
  }

  @Override
  public int getMinSignatures() {
    return minSignatures;
  }

  @Override
  public boolean hasMultipleSenders() {
    return false;
  }

  @Override
  public boolean hasMultipleRecipients() {
    return true;
  }

  public String getServerPrivateKey() {
    return serverPrivateKey;
  }

  public int getMinConfirmations() {
    return minConfirmations;
  }

  public long getGasPrice() {
    return gasPrice;
  }

  public long getContractGas() {
    return contractGas;
  }

  public String getContractAccount() {
    return contractAccount;
  }

  public String getAdminAccount() {
    return adminAccount;
  }

  public String[] getMultiSigAccounts() {
    return multiSigAccounts;
  }

  public BigDecimal getMaxAmountPerHour() {
    return maxAmountPerHour;
  }

  public BigDecimal getMaxAmountPerDay() {
    return maxAmountPerDay;
  }

  public BigDecimal getMaxAmountPerTransaction() {
    return maxAmountPerTransaction;
  }

  public String getContractAddress() {
    return contractAddress;
  }

  public boolean generateNewContract() {
    return generateNewContract;
  }
}
