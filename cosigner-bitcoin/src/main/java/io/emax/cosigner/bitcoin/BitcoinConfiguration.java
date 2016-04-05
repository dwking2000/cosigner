package io.emax.cosigner.bitcoin;

import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.SigningType;
import io.emax.cosigner.api.validation.ValidatorConfiguration;
import io.emax.cosigner.common.EnvironmentVariableParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;

public class BitcoinConfiguration implements CurrencyConfiguration, ValidatorConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(BitcoinConfiguration.class);
  private static String daemonConnectionString = "http://127.0.0.1:18332";
  private static int minConfirmations = 6;
  private static int maxConfirmations = 9999999;
  private static int minSignatures = 1;
  private static String[] multiSigAccounts = {};
  private static int maxDeterministicAddresses = 100;
  private static String daemonUser;
  private static String daemonPassword;
  private static BigDecimal maxAmountPerHour = BigDecimal.ZERO;
  private static BigDecimal maxAmountPerDay = BigDecimal.ZERO;
  private static BigDecimal maxAmountPerTransaction = BigDecimal.ZERO;

  // Ideally we'll prompt for this or something more secure than a properties
  // file...
  private static String serverPrivateKey =
      "b0837faed56bc7c48dc29d564b1c030f03eee53b0317c53d784c8f40654821c6";

  private static boolean configLoaded = false;

  public BitcoinConfiguration() {
    loadConfig();
  }

  private static int getIntProp(Properties prop, String value, int defaultValue) {
    try {
      return Integer.parseInt(prop.getProperty(value));
    } catch (Exception e) {
      LOGGER.warn(null, e);
      return defaultValue;
    }
  }

  private static synchronized void loadConfig() {
    if (!configLoaded) {
      FileInputStream propertiesFile = null;
      String propertiesFilePath = "./cosigner-bitcoin.properties";
      try {
        Properties cosignerProperties = new Properties();
        propertiesFile = new FileInputStream(propertiesFilePath);

        cosignerProperties.load(propertiesFile);
        propertiesFile.close();

        // daemonConnectionString
        daemonConnectionString = EnvironmentVariableParser.resolveEnvVars(
            cosignerProperties.getProperty("daemonConnectionString", daemonConnectionString));

        // minConfirmations
        minConfirmations = getIntProp(cosignerProperties, "minConfirmations", minConfirmations);

        // minSignatures
        minSignatures = getIntProp(cosignerProperties, "minSignatures", minSignatures);

        // maxConfirmations
        maxConfirmations = getIntProp(cosignerProperties, "maxConfirmations", maxConfirmations);

        // multiSigAccounts
        String arrayParser = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("multiSigAccounts"));
        if (arrayParser != null) {
          multiSigAccounts = arrayParser.split("[|]");
        }

        // maxDeterministicAddresses
        maxDeterministicAddresses =
            getIntProp(cosignerProperties, "maxDeterministicAddresses", maxDeterministicAddresses);

        // daemonUser
        daemonUser = cosignerProperties.getProperty("daemonUser", "");

        // daemonPassword
        daemonPassword = cosignerProperties.getProperty("daemonPassword", "");

        // maxAmountPerHour
        maxAmountPerHour = new BigDecimal(
            cosignerProperties.getProperty("maxAmountPerHour", maxAmountPerHour.toPlainString()));

        // maxAmountPerDay
        maxAmountPerDay = new BigDecimal(
            cosignerProperties.getProperty("maxAmountPerDay", maxAmountPerDay.toPlainString()));

        // maxAmountPerTransaction
        maxAmountPerTransaction = new BigDecimal(cosignerProperties
            .getProperty("maxAmountPerTransaction", maxAmountPerTransaction.toPlainString()));

        // serverPrivateKey
        serverPrivateKey = cosignerProperties.getProperty("serverPrivateKey", serverPrivateKey);
        LOGGER.info("cosigner-bitcoin configuration loaded.");
      } catch (IOException e) {
        if (propertiesFile != null) {
          try {
            propertiesFile.close();
          } catch (IOException e1) {
            LOGGER.warn(null, e1);
          }
        }
        LOGGER.info("Could not load cosigner-bitcoin configuration from " + propertiesFilePath
            + ", using defaults.");
      }
      configLoaded = true;
    }
  }

  @Override
  public String getCurrencySymbol() {
    return "BTC";
  }

  @Override
  public SigningType getSigningType() {
    return SigningType.SENDALL;
  }

  public int getMinConfirmations() {
    return minConfirmations;
  }

  public int getMaxConfirmations() {
    return maxConfirmations;
  }

  public String getDaemonConnectionString() {
    return daemonConnectionString;
  }

  public int getMinSignatures() {
    return minSignatures;
  }

  @Override
  public boolean hasMultipleSenders() {
    return true;
  }

  @Override
  public boolean hasMultipleRecipients() {
    return true;
  }

  /**
   * Returns addresses that will be appended to list of signers used in multi-sig address.
   *
   * @return Array of addresses.
   */
  public String[] getMultiSigAccounts() {
    String[] retArray = new String[multiSigAccounts.length];
    System.arraycopy(multiSigAccounts, 0, retArray, 0, multiSigAccounts.length);
    return retArray;
  }

  public String getServerPrivateKey() {
    return serverPrivateKey;
  }

  public String getDaemonUser() {
    return daemonUser;
  }

  public String getDaemonPassword() {
    return daemonPassword;
  }

  public int getMaxDeterministicAddresses() {
    return maxDeterministicAddresses;
  }

  @Override
  public BigDecimal getMaxAmountPerHour() {
    return maxAmountPerHour;
  }

  @Override
  public BigDecimal getMaxAmountPerDay() {
    return maxAmountPerDay;
  }

  @Override
  public BigDecimal getMaxAmountPerTransaction() {
    return maxAmountPerTransaction;
  }
}
