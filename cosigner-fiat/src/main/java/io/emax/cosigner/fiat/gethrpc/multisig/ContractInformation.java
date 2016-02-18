package io.emax.cosigner.fiat.gethrpc.multisig;

public class ContractInformation {
  private String contractAddress;
  private String contractPayload;
  private Class<?> contractVersion;

  /**
   * Create a new instance of contract information.
   */
  public ContractInformation(String contractAddress, String contractPayload,
      Class<?> contractVersion) {
    super();
    this.contractAddress = contractAddress;
    this.contractPayload = contractPayload;
    this.contractVersion = contractVersion;
  }

  public String getContractAddress() {
    return contractAddress;
  }

  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }

  public String getContractPayload() {
    return contractPayload;
  }

  public void setContractPayload(String contractPayload) {
    this.contractPayload = contractPayload;
  }

  public Class<?> getContractVersion() {
    return contractVersion;
  }

  public void setContractVersion(Class<?> contractVersion) {
    this.contractVersion = contractVersion;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((contractAddress == null) ? 0 : contractAddress.hashCode());
    result = prime * result + ((contractPayload == null) ? 0 : contractPayload.hashCode());
    result = prime * result + ((contractVersion == null) ? 0 : contractVersion.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ContractInformation other = (ContractInformation) obj;
    if (contractAddress == null) {
      if (other.contractAddress != null) {
        return false;
      }
    } else if (!contractAddress.equals(other.contractAddress)) {
      return false;
    }
    if (contractPayload == null) {
      if (other.contractPayload != null) {
        return false;
      }
    } else if (!contractPayload.equals(other.contractPayload)) {
      return false;
    }
    if (contractVersion == null) {
      if (other.contractVersion != null) {
        return false;
      }
    } else if (!contractVersion.equals(other.contractVersion)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "ContractInformation [contractAddress=" + contractAddress + ", contractPayload="
        + contractPayload + ", contractVersion=" + contractVersion + "]";
  }

}
