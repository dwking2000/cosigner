package io.emax.cosigner.bitcoin.bitcoindrpc;

import java.math.BigInteger;

import io.emax.cosigner.bitcoin.bitcoindrpc.RawTransaction.VariableInt;
import io.emax.cosigner.bitcoin.common.ByteUtilities;

public final class RawOutput {
  private long amount;
  private long scriptSize = 0;
  private String script = "";

  public long getAmount() {
    return amount;
  }

  public void setAmount(long amount) {
    this.amount = amount;
  }

  public long getScriptSize() {
    return scriptSize;
  }

  public void setScriptSize(long scriptSize) {
    this.scriptSize = scriptSize;
  }

  public String getScript() {
    return script;
  }

  public void setScript(String script) {
    this.script = script;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (amount ^ (amount >>> 32));
    result = prime * result + ((script == null) ? 0 : script.hashCode());
    result = prime * result + (int) (scriptSize ^ (scriptSize >>> 32));
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
    RawOutput other = (RawOutput) obj;
    if (amount != other.amount)
      return false;
    if (script == null) {
      if (other.script != null)
        return false;
    } else if (!script.equals(other.script))
      return false;
    if (scriptSize != other.scriptSize)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "RawOutput [amount=" + amount + ", scriptSize=" + scriptSize + ", script=" + script
        + "]";
  }

  public String encode() {
    String tx = "";

    // Satoshis
    byte[] satoshiBytes =
        ByteUtilities.stripLeadingNullBytes(BigInteger.valueOf(getAmount()).toByteArray());
    satoshiBytes = ByteUtilities.leftPad(satoshiBytes, 8, (byte) 0x00);
    satoshiBytes = ByteUtilities.flipEndian(satoshiBytes);
    tx += ByteUtilities.toHexString(satoshiBytes);

    // Script Size
    setScriptSize(getScript().length() / 2);
    byte[] scriptSizeBytes = RawTransaction.writeVariableInt(getScriptSize());
    tx += ByteUtilities.toHexString(scriptSizeBytes);

    // Script
    byte[] scriptBytes = ByteUtilities.toByteArray(getScript());
    tx += ByteUtilities.toHexString(scriptBytes);

    return tx;
  }

  public static RawOutput parse(String txData) {
    RawOutput output = new RawOutput();
    byte[] rawTx = ByteUtilities.toByteArray(txData);
    int buffPointer = 0;

    byte[] satoshiBytes = ByteUtilities.readBytes(rawTx, buffPointer, 8);
    buffPointer += 8;
    satoshiBytes = ByteUtilities.flipEndian(satoshiBytes);
    output.setAmount(new BigInteger(1, satoshiBytes).longValue());

    VariableInt vScriptSize = RawTransaction.readVariableInt(rawTx, buffPointer);
    buffPointer += vScriptSize.getSize();
    output.setScriptSize(vScriptSize.getValue());

    byte[] scriptBytes = ByteUtilities.readBytes(rawTx, buffPointer, (int) output.getScriptSize());
    output.setScript(ByteUtilities.toHexString(scriptBytes));

    return output;
  }

  public long getDataSize() {
    int sizeSize = RawTransaction.writeVariableInt(getScriptSize()).length;
    // Satoshis + scriptSize + Script
    return 8 + sizeSize + getScriptSize();
  }

  public RawOutput copy() {
    RawOutput output = new RawOutput();

    output.setAmount(getAmount());
    output.setScriptSize(getScriptSize());
    output.setScript(getScript());

    return output;
  }
}
