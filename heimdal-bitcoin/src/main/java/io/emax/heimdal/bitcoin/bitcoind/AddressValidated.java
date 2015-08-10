package io.emax.heimdal.bitcoin.bitcoind;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AddressValidated {
  public enum ScriptType {
    /** P2PK script inside P2SH */
    pubkey, /** P2PKH script inside P2SH */
    pubkeyhash, /** multisig script inside P2SH */
    multisig, /** unknown scripts */
    nonstandard,
  }

  /**
   * Set to true if the address is a valid P2PKH or P2SH address; set to false otherwise
   */
  @JsonProperty("isvalid")
  private boolean valid;
  /**
   * If the address is valid, this is the address base58
   */
  @JsonProperty("address")
  private String address;
  /**
   * Set to true if the address belongs to the wallet; set to false if it does not. Only returned if
   * wallet support enabled
   */
  @JsonProperty("ismine")
  private boolean mine;
  /**
   * Set to true if the address is watch-only. Otherwise set to false. Only returned if address is
   * in the wallet
   */
  @JsonProperty("iswatchonly")
  private boolean watchOnly;
  /**
   * Set to true if a P2SH address; otherwise set to false. Only returned if the address is in the
   * wallet
   */
  @JsonProperty("isscript")
  private boolean script;
  /**
   * Only returned for P2SH addresses belonging to this wallet. This is the type of script:
   */
  @JsonProperty("script")
  private ScriptType scriptType;
  /**
   * Only returned for P2SH addresses belonging to this wallet. This is the redeem script encoded as
   * hex
   */
  @JsonProperty("hex")
  private String hex;

  /**
   * Only returned for P2SH addresses belonging to the wallet. A P2PKH addresses used in this
   * script, or the computed P2PKH addresses of any pubkeys in this script. This array will be empty
   * for nonstandard script types
   */
  @JsonProperty("addresses")
  private List<String> addresses;
  /**
   * Only returned for multisig P2SH addresses belonging to the wallet. The number of signatures
   * required by this script
   */
  @JsonProperty("sigrequired")
  private long requiredSignatures;
  /**
   * The public key corresponding to this address. Only returned if the address is a P2PKH address
   * in the wallet
   */
  @JsonProperty("pubkey")
  private String publicKey;
  /**
   * Set to true if a compressed public key or set to false if an uncompressed public key. Only
   * returned if the address is a P2PKH address in the wallet
   */
  @JsonProperty("iscompressed")
  private boolean compressed;
  /**
   * The account this address belong to. May be an empty string for the default account. Only
   * returned if the address belongs to the wallet
   */
  @JsonProperty("account")
  private String account;

  @Override
  public String toString() {
    return "AddressValidated [valid=" + valid + ", address=" + address + ", mine=" + mine
        + ", watchOnly=" + watchOnly + ", script=" + script + ", scriptType=" + scriptType
        + ", hex=" + hex + ", addresses=" + addresses + ", requiredSignatures=" + requiredSignatures
        + ", publicKey=" + publicKey + ", compressed=" + compressed + ", account=" + account + "]";
  }

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public boolean isMine() {
    return mine;
  }

  public void setMine(boolean mine) {
    this.mine = mine;
  }

  public boolean isWatchOnly() {
    return watchOnly;
  }

  public void setWatchOnly(boolean watchOnly) {
    this.watchOnly = watchOnly;
  }

  public boolean isScript() {
    return script;
  }

  public void setScript(boolean script) {
    this.script = script;
  }

  public ScriptType getScriptType() {
    return scriptType;
  }

  public void setScriptType(ScriptType scriptType) {
    this.scriptType = scriptType;
  }

  public String getHex() {
    return hex;
  }

  public void setHex(String hex) {
    this.hex = hex;
  }

  public List<String> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<String> addresses) {
    this.addresses = addresses;
  }

  public long getRequiredSignatures() {
    return requiredSignatures;
  }

  public void setRequiredSignatures(long requiredSignatures) {
    this.requiredSignatures = requiredSignatures;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  public boolean isCompressed() {
    return compressed;
  }

  public void setCompressed(boolean compressed) {
    this.compressed = compressed;
  }

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }
}
