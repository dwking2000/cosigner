package io.emax.cosigner.bitcoin;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import io.emax.cosigner.api.currency.SigningType;
import io.emax.cosigner.bitcoin.common.EnvironmentVariableParser;

public class CurrencyConfiguration implements io.emax.cosigner.api.currency.CurrencyConfiguration {
  private static String daemonConnectionString = "http://127.0.0.1:18332";
  private static int minConfirmations = 6;
  private static int maxConfirmations = 9999999;
  private static int minSignatures = 1;
  private static String[] multiSigAccounts = {};
  private static int maxDeterministicAddresses = 100;
  // Ideally we'll prompt for this or something more secure than a properties
  // file...
  private static String serverPrivateKey =
      "b0837faed56bc7c48dc29d564b1c030f03eee53b0317c53d784c8f40654821c6";

  private static boolean configLoaded = false;

  public CurrencyConfiguration() {
    loadConfig();
  }

  public synchronized void loadConfig() {
    if (!configLoaded) {
      try {
        String propertiesFilePath = "./cosigner-bitcoin.properties";

        Properties cosignerProperties = new Properties();
        FileInputStream propertiesFile = new FileInputStream(propertiesFilePath);

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
        }

        // minSignatures
        try {
          int intParser = Integer.parseInt(cosignerProperties.getProperty("minSignatures"));
          minSignatures = intParser;
        } catch (NumberFormatException nex) {
        }

        // maxConfirmations
        try {
          int intParser = Integer.parseInt(cosignerProperties.getProperty("maxConfirmations"));
          maxConfirmations = intParser;
        } catch (NumberFormatException nex) {
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
        }

        // serverPrivateKey
        serverPrivateKey = cosignerProperties.getProperty("serverPrivateKey", serverPrivateKey);
        System.out.println("cosigner-bitcoin configuration loaded.");
      } catch (IOException e) {
        System.out.println("Could not load cosigner-bitcoin configuration, using defaults.");
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

  public String[] getMultiSigAccounts() {
    return multiSigAccounts;
  }

  public String getServerPrivateKey() {
    return serverPrivateKey;
  }

  public int getMaxDeterministicAddresses() {
    return maxDeterministicAddresses;
  }
}
