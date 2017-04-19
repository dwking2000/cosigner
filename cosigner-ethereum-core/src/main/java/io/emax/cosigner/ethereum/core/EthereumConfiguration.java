package io.emax.cosigner.ethereum.core;

import io.emax.cosigner.api.currency.CurrencyConfiguration;
import io.emax.cosigner.api.currency.EthereumTransactionConfiguration;
import io.emax.cosigner.api.currency.SigningType;
import io.emax.cosigner.api.validation.ValidatorConfiguration;
import io.emax.cosigner.common.EnvironmentVariableParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;

public class EthereumConfiguration implements CurrencyConfiguration, ValidatorConfiguration,
    EthereumTransactionConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(EthereumConfiguration.class);
  // Defaults
  private String daemonConnectionString = "http://localhost:8101";
  private int minConfirmations = 10;
  private long gasPrice = 100000000000L;
  private long simpleTxGas = 90000L;
  private long contractGas = 3000000L;
  private int minSignatures = 2;
  private String contractKey = "";
  private String contractAccount = "4839540a0ae3242fadf288622f7de1a9278a5858";
  private String[] multiSigKeys = {};
  private String[] multiSigAccounts = {"4839540a0ae3242fadf288622f7de1a9278a5858"};
  private BigDecimal maxAmountPerHour = BigDecimal.ZERO;
  private BigDecimal maxAmountPerDay = BigDecimal.ZERO;
  private BigDecimal maxAmountPerTransaction = BigDecimal.ZERO;
  private String serverPrivateKey =
      "b0837faed56bc7c48dc29d564b1c030f03eee53b0317c53d784c8f40654821c6";

  private boolean configLoaded = false;

  public EthereumConfiguration() {
    loadConfig();
  }

  private static long getLongProp(Properties prop, String value, long defaultValue) {
    try {
      return Long.parseLong(EnvironmentVariableParser.resolveEnvVars(prop.getProperty(value)));
    } catch (Exception e) {
      LOGGER.warn(null, e);
      return defaultValue;
    }
  }

  private synchronized void loadConfig() {
    if (!configLoaded) {
      FileInputStream propertiesFile = null;
      String propertiesFilePath = "./cosigner-ethereum.properties";

      try {
        Properties cosignerProperties = new Properties();
        propertiesFile = new FileInputStream(propertiesFilePath);

        cosignerProperties.load(propertiesFile);
        propertiesFile.close();

        // daemonConnectionString
        daemonConnectionString = EnvironmentVariableParser.resolveEnvVars(
            cosignerProperties.getProperty("daemonConnectionString", daemonConnectionString));

        // minConfirmations
        minConfirmations =
            (int) getLongProp(cosignerProperties, "minConfirmations", minConfirmations);

        // gasPrice
        gasPrice = getLongProp(cosignerProperties, "gasPrice", gasPrice);

        // simpleTxGas
        simpleTxGas = getLongProp(cosignerProperties, "simpleTxGas", simpleTxGas);

        // contractGas
        contractGas = getLongProp(cosignerProperties, "contractGas", contractGas);

        // minSignatures
        minSignatures = (int) getLongProp(cosignerProperties, "minSignatures", minSignatures);

        // contractKey
        contractKey = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("contractKey", contractKey));

        // contractAccount
        contractAccount = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("contractAccount", contractAccount));

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

        // serverPrivateKey
        serverPrivateKey = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("serverPrivateKey", serverPrivateKey));

        LOGGER.info("cosigner-ethereum configuration loaded.");
      } catch (IOException e) {
        if (propertiesFile != null) {
          try {
            propertiesFile.close();
          } catch (IOException e1) {
            LOGGER.warn(null, e1);
          }
        }
        LOGGER.info("Could not load cosigner-ethereum configuration from " + propertiesFilePath
            + ", using defaults.");
      }
      configLoaded = true;
    }
  }

  @Override
  public String getCurrencySymbol() {
    return "ETHR";
  }

  @Override
  public SigningType getSigningType() {
    return SigningType.SENDALL;
  }

  public String getDaemonConnectionString() {
    return daemonConnectionString;
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
    return simpleTxGas;
  }

  @Override
  public long getContractGas() {
    return contractGas;
  }

  public int getMinSignatures() {
    return minSignatures;
  }

  @Override
  public boolean hasMultipleSenders() {
    return false;
  }

  @Override
  public boolean hasMultipleRecipients() {
    return false;
  }

  public String[] getMultiSigKeys() {
    String[] retArray = new String[multiSigKeys.length];
    System.arraycopy(multiSigKeys, 0, retArray, 0, multiSigKeys.length);
    return retArray;
  }

  /**
   * Lists addresses that should be appended to the signers when creating new multi-sig addresses.
   *
   * @return Array of addresses
   */
  public String[] getMultiSigAddresses() {
    String[] retArray = new String[multiSigAccounts.length];
    System.arraycopy(multiSigAccounts, 0, retArray, 0, multiSigAccounts.length);
    return retArray;
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

  public void setGasPrice(long gasPrice) {
    this.gasPrice = gasPrice;
  }

  public void setSimpleTxGas(long simpleTxGas) {
    this.simpleTxGas = simpleTxGas;
  }

  public void setContractGas(long contractGas) {
    this.contractGas = contractGas;
  }
}
