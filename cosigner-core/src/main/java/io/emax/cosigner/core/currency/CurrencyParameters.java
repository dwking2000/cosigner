package io.emax.cosigner.core.currency;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CurrencyParameters {
  @JsonProperty
  private String currencySymbol;

  @JsonProperty("")
  private String userKey;

  @JsonProperty
  private List<String> account;

  @JsonProperty
  private String callback;

  @JsonProperty
  private List<CurrencyParametersRecipient> receivingAccount;

  @JsonProperty
  private String transactionData;

  public String getCurrencySymbol() {
    return currencySymbol;
  }

  public void setCurrencySymbol(String currencySymbol) {
    this.currencySymbol = currencySymbol;
  }

  public String getUserKey() {
    return userKey;
  }

  public void setUserKey(String userKey) {
    this.userKey = userKey;
  }

  public List<String> getAccount() {
    return account;
  }

  public void setAccount(List<String> account) {
    this.account = account;
  }

  public String getCallback() {
    return callback;
  }

  public void setCallback(String callback) {
    this.callback = callback;
  }

  public List<CurrencyParametersRecipient> getReceivingAccount() {
    return receivingAccount;
  }

  public void setReceivingAccount(List<CurrencyParametersRecipient> receivingAccount) {
    this.receivingAccount = receivingAccount;
  }

  public String getTransactionData() {
    return transactionData;
  }

  public void setTransactionData(String transactionData) {
    this.transactionData = transactionData;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((account == null) ? 0 : account.hashCode());
    result = prime * result + ((callback == null) ? 0 : callback.hashCode());
    result = prime * result + ((currencySymbol == null) ? 0 : currencySymbol.hashCode());
    result = prime * result + ((receivingAccount == null) ? 0 : receivingAccount.hashCode());
    result = prime * result + ((transactionData == null) ? 0 : transactionData.hashCode());
    result = prime * result + ((userKey == null) ? 0 : userKey.hashCode());
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
    CurrencyParameters other = (CurrencyParameters) obj;
    if (account == null) {
      if (other.account != null)
        return false;
    } else if (!account.equals(other.account))
      return false;
    if (callback == null) {
      if (other.callback != null)
        return false;
    } else if (!callback.equals(other.callback))
      return false;
    if (currencySymbol == null) {
      if (other.currencySymbol != null)
        return false;
    } else if (!currencySymbol.equals(other.currencySymbol))
      return false;
    if (receivingAccount == null) {
      if (other.receivingAccount != null)
        return false;
    } else if (!receivingAccount.equals(other.receivingAccount))
      return false;
    if (transactionData == null) {
      if (other.transactionData != null)
        return false;
    } else if (!transactionData.equals(other.transactionData))
      return false;
    if (userKey == null) {
      if (other.userKey != null)
        return false;
    } else if (!userKey.equals(other.userKey))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "CurrencyParameters [currencySymbol=" + currencySymbol + ", userKey=" + userKey
        + ", account=" + account + ", callback=" + callback + ", receivingAccount="
        + receivingAccount + ", transactionData=" + transactionData + ", getCurrencySymbol()="
        + getCurrencySymbol() + ", getUserKey()=" + getUserKey() + ", getAccount()=" + getAccount()
        + ", getCallback()=" + getCallback() + ", getReceivingAccount()=" + getReceivingAccount()
        + ", getTransactionData()=" + getTransactionData() + ", hashCode()=" + hashCode()
        + ", getClass()=" + getClass() + ", toString()=" + super.toString() + "]";
  }
}
