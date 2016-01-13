package io.emax.cosigner.ethereum.gethrpc;

import io.emax.cosigner.ethereum.common.Rlp;
import io.emax.cosigner.ethereum.common.RlpEntity;
import io.emax.cosigner.ethereum.common.RlpItem;
import io.emax.cosigner.ethereum.common.RlpList;

public class RawTransaction extends RlpList {
  private static final long serialVersionUID = 1L;

  private final RlpItem nonce = new RlpItem();
  private final RlpItem gasPrice = new RlpItem();
  private final RlpItem gasLimit = new RlpItem();
  private final RlpItem to = new RlpItem();
  private final RlpItem value = new RlpItem();
  private final RlpItem data = new RlpItem();
  private final RlpItem sigV = new RlpItem();
  private final RlpItem sigR = new RlpItem();
  private final RlpItem sigS = new RlpItem();

  /**
   * Raw transaction that is sent over the Ethereum network.
   */
  public RawTransaction() {
    this.add(nonce);
    this.add(gasPrice);
    this.add(gasLimit);
    this.add(to);
    this.add(value);
    this.add(data);
    this.add(sigV);
    this.add(sigR);
    this.add(sigS);
  }

  public static long getSerialversionuid() {
    return serialVersionUID;
  }

  public RlpItem getNonce() {
    return nonce;
  }

  public RlpItem getGasPrice() {
    return gasPrice;
  }

  public RlpItem getGasLimit() {
    return gasLimit;
  }

  public RlpItem getTo() {
    return to;
  }

  public RlpItem getValue() {
    return value;
  }

  public RlpItem getData() {
    return data;
  }

  public RlpItem getSigV() {
    return sigV;
  }

  public RlpItem getSigR() {
    return sigR;
  }

  public RlpItem getSigS() {
    return sigS;
  }

  /**
   * The bytes that need to be signed for the transaction to be valid.
   *
   * @return Byte array of data that will be checked against the signature.
   */
  public byte[] getSigBytes() {
    RlpList sigTx = new RlpList();
    sigTx.add(nonce);
    sigTx.add(gasPrice);
    sigTx.add(gasLimit);
    sigTx.add(to);
    sigTx.add(value);
    sigTx.add(data);

    return sigTx.encode();
  }

  /**
   * Parse a raw transaction from a byte representation into a useful data structure.
   *
   * @param bytes Bytes representing the transaction.
   * @return Data structure built from the raw bytes.
   */
  public static RawTransaction parseBytes(byte[] bytes) {
    RlpEntity entity = Rlp.parseArray(bytes);
    if (!entity.getClass().equals(RlpList.class)) {
      return null;
    }

    RlpList txList = (RlpList) entity;
    RawTransaction tx = new RawTransaction();
    for (int i = 0; i < txList.size(); i++) {
      if (i == 0) {
        tx.getNonce().setDecodedContents(txList.get(i).getDecodedContents());
      } else if (i == 1) {
        tx.getGasPrice().setDecodedContents(txList.get(i).getDecodedContents());
      } else if (i == 2) {
        tx.getGasLimit().setDecodedContents(txList.get(i).getDecodedContents());
      } else if (i == 3) {
        tx.getTo().setDecodedContents(txList.get(i).getDecodedContents());
      } else if (i == 4) {
        tx.getValue().setDecodedContents(txList.get(i).getDecodedContents());
      } else if (i == 5) {
        tx.getData().setDecodedContents(txList.get(i).getDecodedContents());
      } else if (i == 6) {
        tx.getSigV().setDecodedContents(txList.get(i).getDecodedContents());
      } else if (i == 7) {
        tx.getSigR().setDecodedContents(txList.get(i).getDecodedContents());
      } else if (i == 8) {
        tx.getSigS().setDecodedContents(txList.get(i).getDecodedContents());
      }
    }

    return tx;
  }
}
