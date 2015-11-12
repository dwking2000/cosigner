package io.emax.cosigner.bitcoin;

import io.emax.cosigner.api.currency.SigningType;
import io.emax.cosigner.common.EnvironmentVariableParser;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Security;
import java.util.Properties;

public class BitcoinConfiguration implements io.emax.cosigner.api.currency.CurrencyConfiguration {
  private static final Logger logger = LoggerFactory.getLogger(BitcoinConfiguration.class);
  private static String daemonConnectionString = "http://127.0.0.1:18332";
  private static int minConfirmations = 6;
  private static int maxConfirmations = 9999999;
  private static int minSignatures = 1;
  private static String[] multiSigAccounts = {};
  private static int maxDeterministicAddresses = 100;
  private static String daemonUser = "bitcoinrpc";
  private static String daemonPassword = "changeit";

  // Ideally we'll prompt for this or something more secure than a properties
  // file...
  private static String serverPrivateKey =
      "b0837faed56bc7c48dc29d564b1c030f03eee53b0317c53d784c8f40654821c6";

  private static boolean configLoaded = false;

  public BitcoinConfiguration() {
    loadConfig();
    Security.addProvider(new BouncyCastleProvider());
  }

  private static synchronized void loadConfig() {
    if (!configLoaded) {
      FileInputStream propertiesFile = null;
      try {
        String propertiesFilePath = "./cosigner-bitcoin.properties";

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

        // minSignatures
        try {
          int intParser = Integer.parseInt(cosignerProperties.getProperty("minSignatures"));
          minSignatures = intParser;
        } catch (NumberFormatException nex) {
          StringWriter errors = new StringWriter();
          nex.printStackTrace(new PrintWriter(errors));
          logger.warn(errors.toString());
        }

        // maxConfirmations
        try {
          int intParser = Integer.parseInt(cosignerProperties.getProperty("maxConfirmations"));
          maxConfirmations = intParser;
        } catch (NumberFormatException nex) {
          StringWriter errors = new StringWriter();
          nex.printStackTrace(new PrintWriter(errors));
          logger.warn(errors.toString());
        }

        // multiSigAccounts
        String arrayParser = "";
        arrayParser = EnvironmentVariableParser
            .resolveEnvVars(cosignerProperties.getProperty("multiSigAccounts"));
        if (arrayParser != null) {
          multiSigAccounts = arrayParser.split("[|]");
        }

        // maxDeterministicAddresses
        try {
          int intParser =
              Integer.parseInt(cosignerProperties.getProperty("maxDeterministicAddresses"));
          maxDeterministicAddresses = intParser;
        } catch (NumberFormatException nex) {
          StringWriter errors = new StringWriter();
          nex.printStackTrace(new PrintWriter(errors));
          logger.warn(errors.toString());
        }

        // daemonUser
        daemonUser = cosignerProperties.getProperty("daemonUser", daemonUser);

        // daemonPassword
        daemonPassword = cosignerProperties.getProperty("daemonPassword", daemonPassword);

        // serverPrivateKey
        serverPrivateKey = cosignerProperties.getProperty("serverPrivateKey", serverPrivateKey);
        logger.info("cosigner-bitcoin configuration loaded.");
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
        logger.info("Could not load cosigner-bitcoin configuration, using defaults.");
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
}
