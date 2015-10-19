package io.emax.cosigner.bitcoin.bitcoindrpc;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author dquintela
 *
 */
public class Block {
  /**
   * The hash of this block’s block header encoded as hex in RPC byte order
   */
  @JsonProperty("hash")
  private String blockHash;

  /**
   * The number of confirmations the transactions in this block have, starting at 1 when this block
   * is at the tip of the best block chain. This score will be -1 if the the block is not part of
   * the best block chain
   */
  @JsonProperty("confirmations")
  private long confirmations;

  /**
   * The size of this block in serialized block format, counted in bytes
   */
  @JsonProperty("size")
  private int size;

  /**
   * The height of this block on its block chain
   */
  @JsonProperty("height")
  private long height;

  /**
   * This block’s version number. See block version numbers
   */
  @JsonProperty("version")
  private int version;

  /**
   * The merkle root for this block, encoded as hex in RPC byte order
   */
  @JsonProperty("merkleroot")
  private String merkleRoot;

  /**
   * An array containing the TXIDs of all transactions in this block. The transactions appear in the
   * array in the same order they appear in the serialized block
   */
  @JsonProperty("tx")
  private List<String> transactionIds;

  /**
   * The value of the time field in the block header, indicating approximately when the block was
   * created
   */
  @JsonProperty("time")
  private Date time;

  /**
   * The nonce which was successful at turning this particular block into one that could be added to
   * the best block chain
   */
  @JsonProperty("nonce")
  private long nonce;

  /**
   * The value of the nBits field in the block header, indicating the target threshold this block’s
   * header had to pass
   */
  @JsonProperty("bits")
  private String bits;

  /**
   * The estimated amount of work done to find this block relative to the estimated amount of work
   * done to find block 0
   */
  @JsonProperty("difficulty")
  private BigDecimal difficulty;

  /**
   * The estimated number of block header hashes miners had to check from the genesis block to this
   * block, encoded as big-endian hex
   */
  @JsonProperty("chainwork")
  private String chainwork;

  /**
   * The hash of the header of the previous block, encoded as hex in RPC byte order
   */
  @JsonProperty("previousblockhash")
  private String previousBlockHash;

  /**
   * The hash of the next block on the best block chain, if known, encoded as hex in RPC byte order
   */
  @JsonProperty("nextblockhash")
  private String nextBlockHash;

  @Override
  public String toString() {
    return "Block [blockHash=" + blockHash + ", confirmations=" + confirmations + ", size=" + size
        + ", height=" + height + ", version=" + version + ", merkleRoot=" + merkleRoot
        + ", transactionIds=" + transactionIds + ", time=" + time + ", nonce=" + nonce + ", bits="
        + bits + ", difficulty=" + difficulty + ", chainwork=" + chainwork + ", previousBlockHash="
        + previousBlockHash + ", nextBlockHash=" + nextBlockHash + "]";
  }

  /**
   * @return the blockHash
   */
  public String getBlockHash() {
    return blockHash;
  }

  /**
   * @param blockHash the blockHash to set
   */
  public void setBlockHash(String blockHash) {
    this.blockHash = blockHash;
  }

  /**
   * @return the confirmations
   */
  public long getConfirmations() {
    return confirmations;
  }

  /**
   * @param confirmations the confirmations to set
   */
  public void setConfirmations(long confirmations) {
    this.confirmations = confirmations;
  }

  /**
   * @return the size
   */
  public int getSize() {
    return size;
  }

  /**
   * @param size the size to set
   */
  public void setSize(int size) {
    this.size = size;
  }

  /**
   * @return the height
   */
  public long getHeight() {
    return height;
  }

  /**
   * @param height the height to set
   */
  public void setHeight(long height) {
    this.height = height;
  }

  /**
   * @return the version
   */
  public int getVersion() {
    return version;
  }

  /**
   * @param version the version to set
   */
  public void setVersion(int version) {
    this.version = version;
  }

  /**
   * @return the merkleRoot
   */
  public String getMerkleRoot() {
    return merkleRoot;
  }

  /**
   * @param merkleRoot the merkleRoot to set
   */
  public void setMerkleRoot(String merkleRoot) {
    this.merkleRoot = merkleRoot;
  }

  /**
   * @return the transactionIds
   */
  public List<String> getTransactionIds() {
    return transactionIds;
  }

  /**
   * @param transactionIds the transactionIds to set
   */
  public void setTransactionIds(List<String> transactionIds) {
    this.transactionIds = transactionIds;
  }

  /**
   * @return the time
   */
  public Date getTime() {
    return time;
  }

  /**
   * @param time the time to set
   */
  public void setTime(Date time) {
    this.time = time;
  }

  /**
   * @return the nonce
   */
  public long getNonce() {
    return nonce;
  }

  /**
   * @param nonce the nonce to set
   */
  public void setNonce(long nonce) {
    this.nonce = nonce;
  }

  /**
   * @return the bits
   */
  public String getBits() {
    return bits;
  }

  /**
   * @param bits the bits to set
   */
  public void setBits(String bits) {
    this.bits = bits;
  }

  /**
   * @return the difficulty
   */
  public BigDecimal getDifficulty() {
    return difficulty;
  }

  /**
   * @param difficulty the difficulty to set
   */
  public void setDifficulty(BigDecimal difficulty) {
    this.difficulty = difficulty;
  }

  /**
   * @return the chainwork
   */
  public String getChainwork() {
    return chainwork;
  }

  /**
   * @param chainwork the chainwork to set
   */
  public void setChainwork(String chainwork) {
    this.chainwork = chainwork;
  }

  /**
   * @return the previousBlockHash
   */
  public String getPreviousBlockHash() {
    return previousBlockHash;
  }

  /**
   * @param previousBlockHash the previousBlockHash to set
   */
  public void setPreviousBlockHash(String previousBlockHash) {
    this.previousBlockHash = previousBlockHash;
  }

  /**
   * @return the nextBlockHash
   */
  public String getNextBlockHash() {
    return nextBlockHash;
  }

  /**
   * @param nextBlockHash the nextBlockHash to set
   */
  public void setNextBlockHash(String nextBlockHash) {
    this.nextBlockHash = nextBlockHash;
  }
}
