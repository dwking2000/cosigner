package io.emax.cosigner.bitcoin.bitcoindrpc;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SoftForks {
  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * @param version the version to set
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * @return the enforce
   */
  public Status getEnforce() {
    return enforce;
  }

  /**
   * @param enforce the enforce to set
   */
  public void setEnforce(Status enforce) {
    this.enforce = enforce;
  }

  /**
   * @return the reject
   */
  public Status getReject() {
    return reject;
  }

  /**
   * @param reject the reject to set
   */
  public void setReject(Status reject) {
    this.reject = reject;
  }

  public class Status {
    @JsonProperty("status")
    private String status;

    @JsonProperty("found")
    private String found;

    @JsonProperty("required")
    private String required;

    @JsonProperty("window")
    private String window;

    /**
     * @return the status
     */
    public String getStatus() {
      return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
      this.status = status;
    }

    /**
     * @return the found
     */
    public String getFound() {
      return found;
    }

    /**
     * @param found the found to set
     */
    public void setFound(String found) {
      this.found = found;
    }

    /**
     * @return the required
     */
    public String getRequired() {
      return required;
    }

    /**
     * @param required the required to set
     */
    public void setRequired(String required) {
      this.required = required;
    }

    /**
     * @return the window
     */
    public String getWindow() {
      return window;
    }

    /**
     * @param window the window to set
     */
    public void setWindow(String window) {
      this.window = window;
    }
  }

  @JsonProperty("id")
  private String id;

  @JsonProperty("version")
  private String version;

  @JsonProperty("enforce")
  private Status enforce;

  @JsonProperty("reject")
  private Status reject;
}
