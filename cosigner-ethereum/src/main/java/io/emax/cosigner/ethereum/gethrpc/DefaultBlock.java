package io.emax.cosigner.ethereum.gethrpc;

public enum DefaultBlock {

  EARLIEST("earliest"), PENDING("pending"), LATEST("latest");

  private String value;

  DefaultBlock(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return this.getValue();
  }

  /**
   * Provides a mapping from string to enum
   * 
   * @param value String value that the enum represents.
   * @return Enum that corresponds to the string.
   */
  public static DefaultBlock getEnum(String value) {
    for (DefaultBlock v : values()) {
      if (v.getValue().equalsIgnoreCase(value)) {
        return v;
      }
    }
    throw new IllegalArgumentException();
  }
}
