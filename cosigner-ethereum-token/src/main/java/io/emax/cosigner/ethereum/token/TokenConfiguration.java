package io.emax.cosigner.ethereum.token;

import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.EthereumTransactionConfiguration;
import io.emax.cosigner.api.currency.SigningType;
import io.emax.cosigner.common.EnvironmentVariableParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Properties;

public class TokenConfiguration implements CurrencyConfiguration, EthereumTransactionConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(TokenConfiguration.class);

  private String currencySymbol = "TOKEN";
  private long decimalPlaces = 0;
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
  private boolean generateTokenContract = true;
  private boolean useAlternateEtherContract = false;
  private String adminContractAddress = "";
  private String tokenContractAddress = "";
  private String storageContractAddress = "";
  private BigDecimal maxAmountPerHour = BigDecimal.ZERO;
  private BigDecimal maxAmountPerDay = BigDecimal.ZERO;
  private BigDecimal maxAmountPerTransaction = BigDecimal.ZERO;

  private boolean configLoaded = false;

  public TokenConfiguration(String currency) {
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
      LOGGER.info("[" + currency + "] Loading configuration file " + propertiesFilePath);
      try {
        Properties cosignerProperties = new Properties();
        propertiesFile = new FileInputStream(propertiesFilePath);

        cosignerProperties.load(propertiesFile);
        propertiesFile.close();

        // currencySymbol
        currencySymbol = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("currencySymbol", currencySymbol));

        // decimalPlaces
        decimalPlaces = (int) getLongProp(cosignerProperties, "decimalPlaces", decimalPlaces);

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

        // generateTokenContract
        generateTokenContract = Boolean.valueOf(EnvironmentVariableParser.resolveEnvVars(
            cosignerProperties
                .getProperty("generateTokenContract", Boolean.toString(generateTokenContract))));

        // useAlternateEtherContract
        useAlternateEtherContract = Boolean.valueOf(EnvironmentVariableParser.resolveEnvVars(
            cosignerProperties.getProperty("useAlternateEtherContract",
                Boolean.toString(useAlternateEtherContract))));

        // adminContractAddress
        adminContractAddress = EnvironmentVariableParser.resolveEnvVars(
            cosignerProperties.getProperty("adminContractAddress", adminContractAddress));

        // tokenContractAddress
        tokenContractAddress = EnvironmentVariableParser.resolveEnvVars(
            cosignerProperties.getProperty("tokenContractAddress", tokenContractAddress));

        // storageContractAddress
        storageContractAddress = EnvironmentVariableParser.resolveEnvVars(
            cosignerProperties.getProperty("storageContractAddress", storageContractAddress));

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
        LOGGER.info("Could not load cosigner-token configuration from " + propertiesFilePath
            + ", using defaults.");
      }
    }
  }

  @Override
  public String getCurrencySymbol() {
    return currencySymbol;
  }

  public long getDecimalPlaces() {
    return decimalPlaces;
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

  @Override
  public long getGasPrice() {
    return gasPrice;
  }

  @Override
  public long getSimpleTxGas() {
    return contractGas;
  }

  @Override
  public long getContractGas() {
    return contractGas;
  }

  @Override
  public long getWeiMultiplier() {
    return 1000000000000000000L;
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

  public String getAdminContractAddress() {
    return adminContractAddress;
  }

  public String getTokenContractAddress() {
    return tokenContractAddress;
  }

  public String getStorageContractAddress() {
    return storageContractAddress;
  }

  public boolean generateNewContract() {
    return generateNewContract;
  }

  public boolean generateNewContract(boolean val) {
    generateNewContract = val;
    return generateNewContract;
  }

  public boolean generateTokenContract() {
    return generateTokenContract;
  }

  public boolean useAlternateEtherContract() {
    return useAlternateEtherContract;
  }
}
