package io.emax.heimdal.common;

import org.jetbrains.annotations.NotNull;

/**
 * POJO for representing an account balance
 */
public class Balance {

  public final @NotNull String currency;
  public final String amount;
  public final @NotNull String account;
  public final @NotNull Boolean monitored;
  public final BalanceConfirmation firstConfirmation, lastConfirmation;

  public Balance(@NotNull String account, String amount, @NotNull String currency,
      BalanceConfirmation firstConfirmation, BalanceConfirmation lastConfirmation,
      @NotNull Boolean monitored) {
    if (!account.matches("^(0x)?[0-9a-f]+$"))
      throw new RuntimeException("Account is not hexadecimal: " + account);

    if (amount != null && !amount.matches("^(0x)?[0-9a-f]+$"))
      throw new RuntimeException("Amount is not hexadecimal: " + amount);

    this.account = account;
    this.amount = amount;
    this.currency = currency;
    this.firstConfirmation = firstConfirmation;
    this.lastConfirmation = lastConfirmation;
    this.monitored = monitored;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Balance balance = (Balance) o;

    if (!currency.equals(balance.currency))
      return false;
    if (amount != null ? !amount.equals(balance.amount) : balance.amount != null)
      return false;
    if (!account.equals(balance.account))
      return false;
    if (!monitored.equals(balance.monitored))
      return false;
    if (firstConfirmation != null ? !firstConfirmation.equals(balance.firstConfirmation)
        : balance.firstConfirmation != null)
      return false;
    return !(lastConfirmation != null ? !lastConfirmation.equals(balance.lastConfirmation)
        : balance.lastConfirmation != null);

  }

  @Override
  public int hashCode() {
    int result = currency.hashCode();
    result = 31 * result + (amount != null ? amount.hashCode() : 0);
    result = 31 * result + account.hashCode();
    result = 31 * result + monitored.hashCode();
    result = 31 * result + (firstConfirmation != null ? firstConfirmation.hashCode() : 0);
    result = 31 * result + (lastConfirmation != null ? lastConfirmation.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Balance [currency=" + currency + ", amount=" + amount + ", account=" + account
        + ", monitored=" + monitored + ", firstConfirmation=" + firstConfirmation
        + ", lastConfirmation=" + lastConfirmation + "]";
  }
}
