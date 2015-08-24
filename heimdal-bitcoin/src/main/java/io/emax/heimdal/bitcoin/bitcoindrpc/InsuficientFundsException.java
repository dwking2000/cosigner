package io.emax.heimdal.bitcoin.bitcoindrpc;

public class InsuficientFundsException extends RuntimeException {
  /** the serialVersionUID */
  private static final long serialVersionUID = 1L;

  public InsuficientFundsException() {
    super();
  }

  public InsuficientFundsException(String message, Throwable cause) {
    super(message, cause);
  }

  public InsuficientFundsException(String message) {
    super(message);
  }

  public InsuficientFundsException(Throwable cause) {
    super(cause);
  }
}
