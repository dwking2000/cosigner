package io.emax.cosigner.ethereum.gethrpc;

import io.emax.cosigner.ethereum.common.RLP;
import io.emax.cosigner.ethereum.common.RLPEntity;
import io.emax.cosigner.ethereum.common.RLPItem;
import io.emax.cosigner.ethereum.common.RLPList;

public class RawTransaction extends RLPList {
  private static final long serialVersionUID = 1L;

  private RLPItem nonce = new RLPItem();
  private RLPItem gasPrice = new RLPItem();
  private RLPItem gasLimit = new RLPItem();
  private RLPItem to = new RLPItem();
  private RLPItem value = new RLPItem();
  private RLPItem data = new RLPItem();
  private RLPItem sigV = new RLPItem();
  private RLPItem sigR = new RLPItem();
  private RLPItem sigS = new RLPItem();

  public static long getSerialversionuid() {
    return serialVersionUID;
  }

  public RLPItem getNonce() {
    return nonce;
  }

  public RLPItem getGasPrice() {
    return gasPrice;
  }

  public RLPItem getGasLimit() {
    return gasLimit;
  }

  public RLPItem getTo() {
    return to;
  }

  public RLPItem getValue() {
    return value;
  }

  public RLPItem getData() {
    return data;
  }

  public RLPItem getSigV() {
    return sigV;
  }

  public RLPItem getSigR() {
    return sigR;
  }

  public RLPItem getSigS() {
    return sigS;
  }

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

  public byte[] getSigBytes() {
    RLPList sigTx = new RLPList();
    sigTx.add(nonce);
    sigTx.add(gasPrice);
    sigTx.add(gasLimit);
    sigTx.add(to);
    sigTx.add(value);
    sigTx.add(data);

    return sigTx.encode();
  }

  public static RawTransaction parseBytes(byte[] bytes) {
    RLPEntity entity = RLP.parseArray(bytes);
    if (!entity.getClass().equals(RLPList.class)) {
      return null;
    }

    RLPList txList = (RLPList) entity;
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
