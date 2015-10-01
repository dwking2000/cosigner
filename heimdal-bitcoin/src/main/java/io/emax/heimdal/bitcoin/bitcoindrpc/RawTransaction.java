package io.emax.heimdal.bitcoin.bitcoindrpc;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;

import io.emax.heimdal.bitcoin.common.ByteUtilities;

/**
 * Utility class to convert between a raw transaction and the data structure represented here.
 * 
 * @author dorgky
 *
 */
public class RawTransaction {
  /*
   * https://en.bitcoin.it/wiki/Transaction
   * 
   * general format of a Bitcoin transaction (inside a block)
   * 
   * Field Description Size
   * 
   * Version no currently 1 4 bytes
   * 
   * In-counter positive integer VI = VarInt 1 - 9 bytes
   * 
   * list of inputs the * first input of the first transaction is also called "coinbase" (its
   * content was ignored in earlier versions) <in-counter>-many inputs
   * 
   * Out-counter positive integer VI = VarInt 1 - 9 bytes
   * 
   * list of outputs the outputs of the first transaction spend the mined bitcoins for the block
   * <out-counter>-many outputs
   * 
   * lock_time if non-zero and sequence numbers are < 0xFFFFFFFF: block height or timestamp when
   * transaction is final 4 bytes
   */

  /*
   * See: https://en.bitcoin.it/wiki/Script for details on how signing, scripts, etc... work.
   * 
   * Look at re-ordering signatures in msig script in these classes, to make order less relevant
   */

  private int version;
  private long inputCount = 0;
  private LinkedList<RawInput> inputs = new LinkedList<>();
  private long outputCount = 0;
  private LinkedList<RawOutput> outputs = new LinkedList<>();
  private long lockTime;

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public long getInputCount() {
    return inputCount;
  }

  public void setInputCount(long inputCount) {
    this.inputCount = inputCount;
  }

  public LinkedList<RawInput> getInputs() {
    return inputs;
  }

  public void setInputs(LinkedList<RawInput> inputs) {
    this.inputs = inputs;
  }

  public long getOutputCount() {
    return outputCount;
  }

  public void setOutputCount(long outputCount) {
    this.outputCount = outputCount;
  }

  public LinkedList<RawOutput> getOutputs() {
    return outputs;
  }

  public void setOutputs(LinkedList<RawOutput> outputs) {
    this.outputs = outputs;
  }

  public long getLockTime() {
    return lockTime;
  }

  public void setLockTime(long lockTime) {
    this.lockTime = lockTime;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (inputCount ^ (inputCount >>> 32));
    result = prime * result + ((inputs == null) ? 0 : inputs.hashCode());
    result = prime * result + (int) (lockTime ^ (lockTime >>> 32));
    result = prime * result + (int) (outputCount ^ (outputCount >>> 32));
    result = prime * result + ((outputs == null) ? 0 : outputs.hashCode());
    result = prime * result + version;
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
    RawTransaction other = (RawTransaction) obj;
    if (inputCount != other.inputCount)
      return false;
    if (inputs == null) {
      if (other.inputs != null)
        return false;
    } else if (!inputs.equals(other.inputs))
      return false;
    if (lockTime != other.lockTime)
      return false;
    if (outputCount != other.outputCount)
      return false;
    if (outputs == null) {
      if (other.outputs != null)
        return false;
    } else if (!outputs.equals(other.outputs))
      return false;
    if (version != other.version)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "RawTransaction [version=" + version + ", inputCount=" + inputCount + ", inputs="
        + inputs + ", outputCount=" + outputCount + ", outputs=" + outputs + ", lockTime="
        + lockTime + "]";
  }

  /**
   * Returns a String representing the raw tx.
   * 
   * @return
   */
  public String encode() {
    String tx = "";

    // Version
    byte[] versionBytes =
        ByteUtilities.stripLeadingNullBytes(BigInteger.valueOf(getVersion()).toByteArray());
    versionBytes = ByteUtilities.leftPad(versionBytes, 4, (byte)0x00);
    versionBytes = ByteUtilities.flipEndian(versionBytes);
    tx += ByteUtilities.toHexString(versionBytes);

    // Number of inputs
    setInputCount(getInputs().size());
    byte[] inputSizeBytes = writeVariableInt(getInputCount());
    tx += ByteUtilities.toHexString(inputSizeBytes);

    // Inputs
    for (int i = 0; i < getInputCount(); i++) {
      tx += getInputs().get(i).encode();
    }

    // Number of outputs
    setOutputCount(getOutputs().size());
    byte[] outputSizeBytes = writeVariableInt(getOutputCount());
    tx += ByteUtilities.toHexString(outputSizeBytes);

    // Outputs
    for (int i = 0; i < getOutputCount(); i++) {
      tx += getOutputs().get(i).encode();
    }

    // Lock Time
    byte[] lockBytes =
        ByteUtilities.stripLeadingNullBytes(BigInteger.valueOf(getLockTime()).toByteArray());
    lockBytes = ByteUtilities.leftPad(lockBytes, 4, (byte)0x00);
    lockBytes = ByteUtilities.flipEndian(lockBytes);
    tx += ByteUtilities.toHexString(lockBytes);

    return tx;
  }

  /**
   * Decode a raw TX
   * 
   * @param txData
   * @return
   */
  public static RawTransaction parse(String txData) {
    RawTransaction tx = new RawTransaction();
    byte[] rawTx = ByteUtilities.toByteArray(txData);
    int buffPointer = 0;

    // Version
    byte[] version = ByteUtilities.readBytes(rawTx, buffPointer, 4);
    buffPointer += 4;
    version = ByteUtilities.flipEndian(version);
    tx.setVersion(new BigInteger(1, version).intValue());

    // Number of inputs
    VariableInt vInputCount = readVariableInt(rawTx, buffPointer);
    buffPointer += vInputCount.getSize();
    tx.setInputCount(vInputCount.getValue());

    // Parse inputs
    for (long i = 0; i < tx.getInputCount(); i++) {
      byte[] inputData = Arrays.copyOfRange(rawTx, buffPointer, rawTx.length);
      RawInput input = RawInput.parse(ByteUtilities.toHexString(inputData));
      buffPointer += input.getDataSize();
      tx.getInputs().add(input);
    }

    // Get the number of outputs
    VariableInt vOutputCount = readVariableInt(rawTx, buffPointer);
    buffPointer += vOutputCount.getSize();
    tx.setOutputCount(vOutputCount.getValue());

    // Parse outputs
    for (long i = 0; i < tx.getOutputCount(); i++) {
      byte[] outputData = Arrays.copyOfRange(rawTx, buffPointer, rawTx.length);
      RawOutput output = RawOutput.parse(ByteUtilities.toHexString(outputData));
      buffPointer += output.getDataSize();
      tx.getOutputs().add(output);
    }

    // Parse lock time
    byte[] lockBytes = ByteUtilities.readBytes(rawTx, buffPointer, 4);
    buffPointer += 4;
    lockBytes = ByteUtilities.flipEndian(lockBytes);
    tx.setLockTime(new BigInteger(1, lockBytes).longValue());

    return tx;
  }

  public static class VariableInt {
    int size;
    long value;

    public int getSize() {
      return size;
    }

    public void setSize(int size) {
      this.size = size;
    }

    public long getValue() {
      return value;
    }

    public void setValue(long value) {
      this.value = value;
    }
  }

  public static VariableInt readVariableInt(byte[] data, int start) {
    int checkSize = 0xFF & data[start];
    VariableInt vInt = new VariableInt();
    vInt.setSize(0);

    if (checkSize < 0xFD) {
      vInt.setSize(1);
      vInt.setValue(checkSize);
      return vInt;
    }

    if (checkSize == 0xFD) {
      vInt.setSize(3);
    } else if (checkSize == 0xFE) {
      vInt.setSize(5);
    } else if (checkSize == 0xFF) {
      vInt.setSize(9);
    }

    if (vInt.getSize() == 0) {
      return null;
    }

    byte[] newData = ByteUtilities.readBytes(data, start + 1, vInt.getSize() - 1);
    newData = ByteUtilities.flipEndian(newData);
    vInt.setValue(new BigInteger(1, newData).longValue());
    return vInt;
  }

  public static byte[] writeVariableInt(long data) {
    byte[] newData = new byte[0];

    if (data < 0xFD) {
      newData = new byte[1];
      newData[0] = (byte) (data & 0xFF);
    } else if (data <= 0xFFFF) {
      newData = new byte[3];
      newData[0] = (byte) 0xFD;
    } else if (data <= 0xFFFFFFFF) {
      newData = new byte[5];
      newData[0] = (byte) 0xFE;
    } else {
      newData = new byte[9];
      newData[0] = (byte) 0xFF;
    }

    byte[] intData = BigInteger.valueOf(data).toByteArray();
    intData = ByteUtilities.stripLeadingNullBytes(intData);
    intData = ByteUtilities.leftPad(intData, newData.length - 1, (byte)0x00);
    intData = ByteUtilities.flipEndian(intData);

    for (int i = 0; i < (newData.length - 1); i++) {
      newData[i + 1] = intData[i];
    }

    return newData;
  }
}
