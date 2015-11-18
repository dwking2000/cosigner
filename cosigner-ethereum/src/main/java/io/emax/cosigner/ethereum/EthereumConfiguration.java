package io.emax.cosigner.ethereum;

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

public class EthereumConfiguration implements CurrencyConfiguration, ValidatorConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(EthereumConfiguration.class);
  // Defaults
  private static String daemonConnectionString = "http://localhost:8101";
  private static int minConfirmations = 10;
  private static long gasPrice = 100000000000L;
  private static long simpleTxGas = 90000L;
  private static long contractGas = 3000000L;
  private static int minSignatures = 2;
  private static String contractAccount = "4839540a0ae3242fadf288622f7de1a9278a5858";
  private static String[] multiSigAccounts = {"4839540a0ae3242fadf288622f7de1a9278a5858"};
  private static BigDecimal maxAmountPerHour;
  private static BigDecimal maxAmountPerDay;
  private static BigDecimal maxAmountPerTransaction;
  // Ideally we'll prompt for this or something more secure than a properties
  // file...
  private static String serverPrivateKey =
      "b0837faed56bc7c48dc29d564b1c030f03eee53b0317c53d784c8f40654821c6";

  private static boolean configLoaded = false;

  public EthereumConfiguration() {
    loadConfig();
  }

  private static long getLongProp(Properties prop, String value, long defaultValue) {
    try {
      return Long.parseLong(prop.getProperty(value));
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

        // contractAccount
        contractAccount = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("contractAccount", contractAccount));

        // multiSigAccounts
        String arrayParser = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("multiSigAccounts"));
        if (arrayParser != null) {
          multiSigAccounts = arrayParser.split("[|]");
        }

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

        LOGGER.info("cosigner-ethereum configuration loaded.");
      } catch (IOException e) {
        if (propertiesFile != null) {
          try {
            propertiesFile.close();
          } catch (IOException e1) {
            LOGGER.warn(null, e1);
          }
        }
        LOGGER.debug(null, e);
        LOGGER.info("Could not load cosigner-ethereum configuration, using defaults.");
      }
      configLoaded = true;
    }
  }

  @Override
  public String getCurrencySymbol() {
    return "ETH";
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

  public long getGasPrice() {
    return gasPrice;
  }

  public long getSimpleTxGas() {
    return simpleTxGas;
  }

  public long getContractGas() {
    return contractGas;
  }

  public int getMinSignatures() {
    return minSignatures;
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

  public long getWeiMultiplier() {
    return 1000000000000000000L;
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
}
