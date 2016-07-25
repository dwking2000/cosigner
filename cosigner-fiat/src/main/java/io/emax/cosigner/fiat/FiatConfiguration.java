package io.emax.cosigner.fiat;

import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.SigningType;
import io.emax.cosigner.common.EnvironmentVariableParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Properties;

public class FiatConfiguration implements CurrencyConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(FiatConfiguration.class);

  private String currencySymbol = "FIAT";
  // TODO Encrypt if possible.
  private String serverPrivateKey = "deadbeef";
  private int minSignatures = 10;
  private int minConfirmations = 10;
  private long gasPrice = 100000000000L;
  private long contractGas = 3000000L;
  private String contractKey = "";
  private String contractAccount = "4839540a0ae3242fadf288622f7de1a9278a5858";
  private String adminAccount = "4839540a0ae3242fadf288622f7de1a9278a5858";
  private String[] multiSigKeys = {};
  private String[] multiSigAccounts = {"4839540a0ae3242fadf288622f7de1a9278a5858"};
  private boolean generateNewContract = true;
  private String contractAddress = "";
  private BigDecimal maxAmountPerHour = BigDecimal.ZERO;
  private BigDecimal maxAmountPerDay = BigDecimal.ZERO;
  private BigDecimal maxAmountPerTransaction = BigDecimal.ZERO;

  private boolean configLoaded = false;

  public FiatConfiguration(String currency) {
    loadConfig(currency);
  }

  private static long getLongProp(Properties prop, String value, long defaultValue) {
    try {
      return Long.parseLong(EnvironmentVariableParser.resolveEnvVars(prop.getProperty(value)));
    } catch (Exception e) {
      LOGGER.warn(null, e);
      return defaultValue;
    }
  }

  private synchronized void loadConfig(String currency) {
    if (!configLoaded) {
      configLoaded = true;

      FileInputStream propertiesFile = null;
      String propertiesFilePath = "./cosigner-" + currency.toLowerCase(Locale.US) + ".properties";

      try {
        Properties cosignerProperties = new Properties();
        propertiesFile = new FileInputStream(propertiesFilePath);

        cosignerProperties.load(propertiesFile);
        propertiesFile.close();

        // currencySymbol
        currencySymbol = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("currencySymbol", currencySymbol));

        // serverPrivateKey
        serverPrivateKey = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("serverPrivateKey", serverPrivateKey));

        // minSignatures
        minSignatures = (int) getLongProp(cosignerProperties, "minSignatures", minSignatures);

        // minConfirmations
        minConfirmations =
            (int) getLongProp(cosignerProperties, "minConfirmations", minConfirmations);

        // gasPrice
        gasPrice = getLongProp(cosignerProperties, "gasPrice", gasPrice);

        // contractGas
        contractGas = getLongProp(cosignerProperties, "contractGas", contractGas);

        // contractKey
        contractKey = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("contractKey", contractKey));

        // contractAccount
        contractAccount = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("contractAccount", contractAccount));

        // adminAccount
        adminAccount = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("adminAccount", adminAccount));

        // multiSigKeys
        String arrayParser = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("multiSigKeys"));
        if (arrayParser != null) {
          multiSigKeys = arrayParser.split("[|]");
        }

        // multiSigAccounts
        arrayParser = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("multiSigAccounts"));
        if (arrayParser != null) {
          multiSigAccounts = arrayParser.split("[|]");
        }

        // generateNewContract
        generateNewContract = Boolean.valueOf(EnvironmentVariableParser.resolveEnvVars(
            cosignerProperties
                .getProperty("generateNewContract", Boolean.toString(generateNewContract))));

        // contractAddress
        contractAddress = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("contractAddress", contractAddress));

        // maxAmountPerHour
        maxAmountPerHour = new BigDecimal(EnvironmentVariableParser.resolveEnvVars(
            cosignerProperties.getProperty("maxAmountPerHour", maxAmountPerHour.toPlainString())));

        // maxAmountPerDay
        maxAmountPerDay = new BigDecimal(EnvironmentVariableParser.resolveEnvVars(
            cosignerProperties.getProperty("maxAmountPerDay", maxAmountPerDay.toPlainString())));

        // maxAmountPerTransaction
        maxAmountPerTransaction = new BigDecimal(EnvironmentVariableParser.resolveEnvVars(
            cosignerProperties
                .getProperty("maxAmountPerTransaction", maxAmountPerTransaction.toPlainString())));

      } catch (IOException e) {
        if (propertiesFile != null) {
          try {
            propertiesFile.close();
          } catch (IOException e1) {
            LOGGER.warn(null, e1);
          }
        }
        LOGGER.info("Could not load cosigner-fiat configuration from " + propertiesFilePath
            + ", using defaults.");
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

  public String getContractKey() {
    return contractKey;
  }

  public String getContractAccount() {
    return contractAccount;
  }

  public String getAdminAccount() {
    return adminAccount;
  }

  public String[] getMultiSigKeys() {
    return multiSigKeys;
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
