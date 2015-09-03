package io.emax.heimdal.ethereum.gethrpc;

import java.math.BigInteger;
import java.util.LinkedList;

import org.bouncycastle.util.Arrays;

import io.emax.heimdal.ethereum.common.ByteUtilities;

public class MultiSigContractParameters {
  private String function;
  private BigInteger nonce;
  private LinkedList<String> address = new LinkedList<>();
  private LinkedList<BigInteger> value = new LinkedList<>();
  private LinkedList<BigInteger> sigV = new LinkedList<>();
  private LinkedList<BigInteger> sigR = new LinkedList<>();
  private LinkedList<BigInteger> sigS = new LinkedList<>();

  // Getters & Setters
  public LinkedList<String> getAddress() {
    return address;
  }

  public void setAddress(LinkedList<String> address) {
    this.address = address;
  }

  public LinkedList<BigInteger> getValue() {
    return value;
  }

  public void setValue(LinkedList<BigInteger> value) {
    this.value = value;
  }

  public BigInteger getNonce() {
    return nonce;
  }

  public void setNonce(BigInteger nonce) {
    this.nonce = nonce;
  }

  public LinkedList<BigInteger> getSigV() {
    return sigV;
  }

  public void setSigV(LinkedList<BigInteger> sigV) {
    this.sigV = sigV;
  }

  public LinkedList<BigInteger> getSigR() {
    return sigR;
  }

  public void setSigR(LinkedList<BigInteger> sigR) {
    this.sigR = sigR;
  }

  public LinkedList<BigInteger> getSigS() {
    return sigS;
  }

  public void setSigS(LinkedList<BigInteger> sigS) {
    this.sigS = sigS;
  }

  public String getFunction() {
    return function;
  }

  public void setFunction(String function) {
    this.function = function;
  }

  // Encode
  public byte[] encode() {
    String encodedData = this.function;
    // nonce
    encodedData += String.format("%64s", ByteUtilities.toHexString(this.nonce.toByteArray()))
        .replace(' ', '0');

    // address
    encodedData +=
        String.format("%64s", ByteUtilities.toHexString(BigInteger.valueOf(6 * 32).toByteArray()))
            .replace(' ', '0');

    // value
    encodedData +=
        String
            .format("%64s",
                ByteUtilities.toHexString(
                    BigInteger.valueOf(7 * 32 + 32 * this.address.size()).toByteArray()))
        .replace(' ', '0');

    // sigV
    encodedData += String
        .format("%64s",
            ByteUtilities.toHexString(BigInteger
                .valueOf(8 * 32 + 32 * (this.address.size() + this.value.size())).toByteArray()))
        .replace(' ', '0');

    // sigR
    encodedData +=
        String
            .format("%64s",
                ByteUtilities.toHexString(BigInteger
                    .valueOf(
                        9 * 32 + 32 * (this.address.size() + this.value.size() + this.sigV.size()))
                    .toByteArray()))
            .replace(' ', '0');

    // sigS
    encodedData +=
        String
            .format("%64s",
                ByteUtilities.toHexString(BigInteger.valueOf(10 * 32 + 32 * (this.address.size()
                    + this.value.size() + this.sigV.size() + this.sigR.size())).toByteArray()))
        .replace(' ', '0');

    // address[]
    encodedData += String
        .format("%64s",
            ByteUtilities.toHexString(ByteUtilities
                .stripLeadingNullBytes(BigInteger.valueOf(this.address.size()).toByteArray())))
        .replace(' ', '0');
    for (String address : address) {
      encodedData += String.format("%64s", address).replace(' ', '0');
    }

    // value[]
    encodedData += String
        .format("%64s",
            ByteUtilities.toHexString(ByteUtilities
                .stripLeadingNullBytes(BigInteger.valueOf(this.value.size()).toByteArray())))
        .replace(' ', '0');
    for (BigInteger value : value) {
      encodedData +=
          String
              .format("%64s",
                  ByteUtilities
                      .toHexString(ByteUtilities.stripLeadingNullBytes(value.toByteArray())))
              .replace(' ', '0');
    }

    // sigV[]
    encodedData += String
        .format("%64s",
            ByteUtilities.toHexString(ByteUtilities
                .stripLeadingNullBytes(BigInteger.valueOf(this.sigV.size()).toByteArray())))
        .replace(' ', '0');
    for (BigInteger sigv : sigV) {
      encodedData +=
          String
              .format("%64s",
                  ByteUtilities
                      .toHexString(ByteUtilities.stripLeadingNullBytes(sigv.toByteArray())))
              .replace(' ', '0');
    }

    // sigR[]
    encodedData += String
        .format("%64s",
            ByteUtilities.toHexString(ByteUtilities
                .stripLeadingNullBytes(BigInteger.valueOf(this.sigR.size()).toByteArray())))
        .replace(' ', '0');
    for (BigInteger sigr : sigR) {
      encodedData +=
          String
              .format("%64s",
                  ByteUtilities
                      .toHexString(ByteUtilities.stripLeadingNullBytes(sigr.toByteArray())))
              .replace(' ', '0');
    }

    // sigS[]
    encodedData += String
        .format("%64s",
            ByteUtilities.toHexString(ByteUtilities
                .stripLeadingNullBytes(BigInteger.valueOf(this.sigS.size()).toByteArray())))
        .replace(' ', '0');
    for (BigInteger sigs : sigS) {
      encodedData +=
          String
              .format("%64s",
                  ByteUtilities
                      .toHexString(ByteUtilities.stripLeadingNullBytes(sigs.toByteArray())))
              .replace(' ', '0');
    }

    return ByteUtilities.toByteArray(encodedData);
  }

  // Decode
  public MultiSigContractParameters decode(byte[] data) {
    int buffPointer = 0;

    // First 16-bytes should be function
    byte[] functionBytes = Arrays.copyOfRange(data, buffPointer, buffPointer + 4);
    buffPointer += 4;
    this.function = ByteUtilities.toHexString(functionBytes);

    // 32 for nonce
    byte[] nonceBytes = Arrays.copyOfRange(data, buffPointer, buffPointer + 32);
    buffPointer += 32;
    this.nonce = new BigInteger(1, nonceBytes);

    // address
    byte[] addressBytes = Arrays.copyOfRange(data, buffPointer, buffPointer + 32);
    buffPointer += 32;
    BigInteger addressPointer = new BigInteger(1, addressBytes);
    addressPointer = addressPointer.add(BigInteger.valueOf(4));
    // Get the size
    addressBytes = Arrays.copyOfRange(data, addressPointer.intValue(), addressPointer.intValue() + 32);
    BigInteger addressSize = new BigInteger(1, addressBytes);
    // Loop over each entry and load it up.
    this.address.clear();
    for (int i = 0; i < addressSize.intValue(); i++) {
      int dataLocation = addressPointer.intValue() + 32 + (i * 32);
      addressBytes = Arrays.copyOfRange(data, dataLocation, dataLocation + 32);
      this.address.add(ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(addressBytes)));
    }
    
    // 32 for value
    byte[] valueBytes = Arrays.copyOfRange(data, buffPointer, buffPointer + 32);
    buffPointer += 32;
    BigInteger valuePointer = new BigInteger(1, valueBytes);
    valuePointer = valuePointer.add(BigInteger.valueOf(4));
    // Get the size
    valueBytes = Arrays.copyOfRange(data, valuePointer.intValue(), valuePointer.intValue() + 32);
    BigInteger valueSize = new BigInteger(1, valueBytes);
    // Loop over each entry and load it up.
    this.value.clear();
    for (int i = 0; i < valueSize.intValue(); i++) {
      int dataLocation = valuePointer.intValue() + 32 + (i * 32);
      valueBytes = Arrays.copyOfRange(data, dataLocation, dataLocation + 32);
      this.value.add(new BigInteger(1, valueBytes));
    }


    // 32 for sigV array pointer
    // Get the pointer
    byte[] sigVBytes = Arrays.copyOfRange(data, buffPointer, buffPointer + 32);
    buffPointer += 32;
    BigInteger sigVpointer = new BigInteger(1, sigVBytes);
    sigVpointer = sigVpointer.add(BigInteger.valueOf(4));
    // Get the size
    sigVBytes = Arrays.copyOfRange(data, sigVpointer.intValue(), sigVpointer.intValue() + 32);
    BigInteger sigVsize = new BigInteger(1, sigVBytes);
    // Loop over each entry and load it up.
    this.sigV.clear();
    for (int i = 0; i < sigVsize.intValue(); i++) {
      int dataLocation = sigVpointer.intValue() + 32 + (i * 32);
      sigVBytes = Arrays.copyOfRange(data, dataLocation, dataLocation + 32);
      this.sigV.add(new BigInteger(1, sigVBytes));
    }

    // 32 for sigR array pointer
    byte[] sigRBytes = Arrays.copyOfRange(data, buffPointer, buffPointer + 32);
    buffPointer += 32;
    BigInteger sigRpointer = new BigInteger(1, sigRBytes);
    sigRpointer = sigRpointer.add(BigInteger.valueOf(4));
    // Get the size
    sigRBytes = Arrays.copyOfRange(data, sigRpointer.intValue(), sigRpointer.intValue() + 32);
    BigInteger sigRsize = new BigInteger(1, sigRBytes);
    // Loop over each entry and load it up.
    this.sigR.clear();
    for (int i = 0; i < sigRsize.intValue(); i++) {
      int dataLocation = sigRpointer.intValue() + 32 + (i * 32);
      sigRBytes = Arrays.copyOfRange(data, dataLocation, dataLocation + 32);
      this.sigR.add(new BigInteger(1, sigRBytes));
    }

    // 32 for sigS array pointer
    byte[] sigSBytes = Arrays.copyOfRange(data, buffPointer, buffPointer + 32);
    buffPointer += 32;
    BigInteger sigSpointer = new BigInteger(1, sigSBytes);
    sigSpointer = sigSpointer.add(BigInteger.valueOf(4));
    // Get the size
    sigSBytes = Arrays.copyOfRange(data, sigSpointer.intValue(), sigSpointer.intValue() + 32);
    BigInteger sigSsize = new BigInteger(1, sigSBytes);
    // Loop over each entry and load it up.
    this.sigS.clear();
    for (int i = 0; i < sigSsize.intValue(); i++) {
      int dataLocation = sigSpointer.intValue() + 32 + (i * 32);
      sigSBytes = Arrays.copyOfRange(data, dataLocation, dataLocation + 32);
      this.sigS.add(new BigInteger(1, sigSBytes));
    }

    return this;
  }
}
