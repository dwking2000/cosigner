package io.emax.cosigner.common;

public class GlobalConfig {
  private static String persistenceLocation = "mem:txs";

  public static String getPersistenceLocation() {
    return persistenceLocation;
  }

  public static void setPersistenceLocation(String persistenceLocation) {
    GlobalConfig.persistenceLocation = persistenceLocation;
  }
}
