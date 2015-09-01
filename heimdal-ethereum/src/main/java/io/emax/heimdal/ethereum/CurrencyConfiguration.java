package io.emax.heimdal.ethereum;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import io.emax.heimdal.api.currency.SigningType;
import io.emax.heimdal.ethereum.common.EnvironmentVariableParser;

public class CurrencyConfiguration implements io.emax.heimdal.api.currency.CurrencyConfiguration {
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

  public CurrencyConfiguration() {
    loadConfig();
  }

  private synchronized void loadConfig() {
    if (!configLoaded) {
      String propertiesFilePath = "./heimdal-ethereum.properties";

      try {
        Properties heimdalProperties = new Properties();
        FileInputStream propertiesFile = new FileInputStream(propertiesFilePath);

        heimdalProperties.load(propertiesFile);
        propertiesFile.close();

        // daemonConnectionString
        daemonConnectionString = EnvironmentVariableParser.resolveEnvVars(
            heimdalProperties.getProperty("daemonConnectionString", daemonConnectionString));

        // minConfirmations
        try {
          int intParser = Integer.parseInt(heimdalProperties.getProperty("minConfirmations"));
          minConfirmations = intParser;
        } catch (NumberFormatException nex) {
        }

        // gasPrice
        try {
          long longParser = Long.parseLong(heimdalProperties.getProperty("gasPrice"));
          gasPrice = longParser;
        } catch (NumberFormatException nex) {
        }

        // simpleTxGas
        try {
          long longParser = Long.parseLong(heimdalProperties.getProperty("simpleTxGas"));
          simpleTxGas = longParser;
        } catch (NumberFormatException nex) {
        }

        // contractGas
        try {
          long longParser = Long.parseLong(heimdalProperties.getProperty("contractGas"));
          contractGas = longParser;
        } catch (NumberFormatException nex) {
        }

        // minSignatures
        try {
          int intParser = Integer.parseInt(heimdalProperties.getProperty("minSignatures"));
          minSignatures = intParser;
        } catch (NumberFormatException nex) {
        }

        // contractAccount
        contractAccount = heimdalProperties.getProperty("contractAccount", contractAccount);

        // multiSigAccounts
        String arrayParser = "";
        arrayParser = heimdalProperties.getProperty("multiSigAccounts");
        if (arrayParser != null) {
          multiSigAccounts = arrayParser.split("[|]");
        }

        // serverPrivateKey
        serverPrivateKey = heimdalProperties.getProperty("serverPrivateKey", serverPrivateKey);

        System.out.println("heimdal-ethereum configuration loaded.");
      } catch (IOException e) {
        System.out.println("Could not load heimdal-ethereum configuration, using defaults.");
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
    return minConfirmations; // TODO don't use 10
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

  public String[] getMultiSigAddresses() {
    return multiSigAccounts;
  }

  public long getWeiMultiplier() {
    return 1000000000000000000L;
  }

  public String getContractAccount() {
    return contractAccount;
  }
}
