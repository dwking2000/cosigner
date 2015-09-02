package io.emax.heimdal.core.currency;

public class CurrencyParametersRecipient {
  private String recipientAddress;
  private String amount;

  public String getRecipientAddress() {
    return recipientAddress;
  }

  public void setRecipientAddress(String recipientAddress) {
    this.recipientAddress = recipientAddress;
  }

  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((amount == null) ? 0 : amount.hashCode());
    result = prime * result + ((recipientAddress == null) ? 0 : recipientAddress.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    CurrencyParametersRecipient other = (CurrencyParametersRecipient) obj;
    if (amount == null) {
      if (other.amount != null)
        return false;
    } else if (!amount.equals(other.amount))
      return false;
    if (recipientAddress == null) {
      if (other.recipientAddress != null)
        return false;
    } else if (!recipientAddress.equals(other.recipientAddress))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "CurrencyParametersRecipient [recipientAddress=" + recipientAddress + ", amount="
        + amount + "]";
  }
}
