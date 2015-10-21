package io.emax.cosigner.bitcoin.bitcoindrpc;

public enum NetworkBytes {
  
  P2SH("05"), P2PKH("00"), PRIVATEKEY("80"), P2SH_TEST("C4"), P2PKH_TEST("6F"), PRIVATEKEY_TEST("EF");

  private String value;
  
  NetworkBytes(String value) {
    this.value = value;
  }
  
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return this.getValue();
  }

  public static NetworkBytes getEnum(String value) {
    for (NetworkBytes v : values())
      if (v.getValue().equalsIgnoreCase(value))
        return v;
    throw new IllegalArgumentException();
  }
}
