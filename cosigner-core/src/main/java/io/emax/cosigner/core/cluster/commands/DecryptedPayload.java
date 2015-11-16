package io.emax.cosigner.core.cluster.commands;

import io.emax.cosigner.common.Json;

public class DecryptedPayload {
  private long nonce;
  private String payload;

  public long getNonce() {
    return nonce;
  }

  public void setNonce(long nonce) {
    this.nonce = nonce;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public String toJson() {
    return Json.stringifyObject(DecryptedPayload.class, this);
  }

  public static DecryptedPayload parseData(String data) {
    return (DecryptedPayload) Json.objectifyString(DecryptedPayload.class, data);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (nonce ^ (nonce >>> 32));
    result = prime * result + ((payload == null) ? 0 : payload.hashCode());
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
    DecryptedPayload other = (DecryptedPayload) obj;
    if (nonce != other.nonce) {
      return false;
    }
    if (payload == null) {
      if (other.payload != null) {
        return false;
      }
    } else if (!payload.equals(other.payload)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "DecryptedPayload [nonce=" + nonce + ", payload=" + payload + "]";
  }

}
