package io.emax.cosigner.ethereum;

import io.emax.cosigner.api.currency.SigningType;
import io.emax.cosigner.common.EnvironmentVariableParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

public class EthereumConfiguration implements io.emax.cosigner.api.currency.CurrencyConfiguration {
  private static final Logger logger = LoggerFactory.getLogger(EthereumConfiguration.class);
  // Defaults
  private static String daemonConnectionString = "http://localhost:8101";
  private static int minConfirmations = 10;
  private static long gasPrice = 100000000000L;
  private static long simpleTxGas = 90000L;
  private static long contractGas = 3000000L;
  private static int minSignatures = 2;
  private static String contractAccount = "4839540a0ae3242fadf288622f7de1a9278a5858";
  private static String[] multiSigAccounts = {"4839540a0ae3242fadf288622f7de1a9278a5858"};
  // Ideally we'll prompt for this or something more secure than a properties
  // file...
  private static String serverPrivateKey =
      "b0837faed56bc7c48dc29d564b1c030f03eee53b0317c53d784c8f40654821c6";

  private static boolean configLoaded = false;

  public EthereumConfiguration() {
    loadConfig();
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
        try {
          int intParser = Integer.parseInt(cosignerProperties.getProperty("minConfirmations"));
          minConfirmations = intParser;
        } catch (NumberFormatException nex) {
          StringWriter errors = new StringWriter();
          nex.printStackTrace(new PrintWriter(errors));
          logger.warn(errors.toString());
        }

        // gasPrice
        try {
          long longParser = Long.parseLong(cosignerProperties.getProperty("gasPrice"));
          gasPrice = longParser;
        } catch (NumberFormatException nex) {
          StringWriter errors = new StringWriter();
          nex.printStackTrace(new PrintWriter(errors));
          logger.warn(errors.toString());
        }

        // simpleTxGas
        try {
          long longParser = Long.parseLong(cosignerProperties.getProperty("simpleTxGas"));
          simpleTxGas = longParser;
        } catch (NumberFormatException nex) {
          StringWriter errors = new StringWriter();
          nex.printStackTrace(new PrintWriter(errors));
          logger.warn(errors.toString());
        }

        // contractGas
        try {
          long longParser = Long.parseLong(cosignerProperties.getProperty("contractGas"));
          contractGas = longParser;
        } catch (NumberFormatException nex) {
          StringWriter errors = new StringWriter();
          nex.printStackTrace(new PrintWriter(errors));
          logger.warn(errors.toString());
        }

        // minSignatures
        try {
          int intParser = Integer.parseInt(cosignerProperties.getProperty("minSignatures"));
          minSignatures = intParser;
        } catch (NumberFormatException nex) {
          StringWriter errors = new StringWriter();
          nex.printStackTrace(new PrintWriter(errors));
          logger.warn(errors.toString());
        }

        // contractAccount
        contractAccount = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("contractAccount", contractAccount));

        // multiSigAccounts
        String arrayParser = "";
        arrayParser = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("multiSigAccounts"));
        if (arrayParser != null) {
          multiSigAccounts = arrayParser.split("[|]");
        }

        // serverPrivateKey
        serverPrivateKey = cosignerProperties.getProperty("serverPrivateKey", serverPrivateKey);

        logger.info("cosigner-ethereum configuration loaded.");
      } catch (IOException e) {
        if (propertiesFile != null) {
          try {
            propertiesFile.close();
          } catch (IOException e1) {
            StringWriter errors = new StringWriter();
            e1.printStackTrace(new PrintWriter(errors));
            logger.warn(errors.toString());
          }
        }
        logger.info("Could not load cosigner-ethereum configuration, using defaults.");
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
}
