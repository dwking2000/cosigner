package io.emax.heimdal.bitcoin.bitcoindrpc;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Information about the current state of the local block chain
 * 
 * @author dquintela
 */
public class BlockChainInfo {
  /**
   * The name of the block chain. One of main for mainnet, test for testnet, or regtest for regtest
   */
  @JsonProperty("chain")
  private BlockChainName chain;

  /**
   * The number of validated blocks in the local best block chain. For a new node with just the
   * hardcoded genesis block, this will be 0
   */
  @JsonProperty("blocks")
  private long blocks;

  /**
   * Added in Bitcoin Core 0.10.0
   * 
   * The number of validated headers in the local best headers chain. For a new node with just the
   * hardcoded genesis block, this will be zero. This number may be higher than the number of blocks
   */
  @JsonProperty("headers")
  private long headers;

  /**
   * The hash of the header of the highest validated block in the best block chain, encoded as hex
   * in RPC byte order. This is identical to the string returned by the getbestblockhash RPC
   */
  @JsonProperty("bestblockhash")
  private String bestBlockHash;

  /**
   * The difficulty of the highest-height block in the best block chain
   */
  @JsonProperty("difficulty")
  private BigDecimal difficulty;

  /**
   * Estimate of what percentage of the block chain transactions have been verified so far, starting
   * at 0.0 and increasing to 1.0 for fully verified. May slightly exceed 1.0 when fully synced to
   * account for transactions in the memory pool which have been verified before being included in a
   * block
   */
  @JsonProperty("verificationprogress")
  private BigDecimal verificationProgress;

  /**
   * The estimated number of block header hashes checked from the genesis block to this block,
   * encoded as big-endian hex
   */
  @JsonProperty("chainwork")
  private String chainwork;

  @Override
  public String toString() {
    return "BlockChainInfo [chain=" + chain + ", blocks=" + blocks + ", headers=" + headers
        + ", bestBlockHash=" + bestBlockHash + ", difficulty=" + difficulty
        + ", verificationProgress=" + verificationProgress + ", chainwork=" + chainwork + "]";
  }

  /**
   * @return the chain
   */
  public BlockChainName getChain() {
    return chain;
  }

  /**
   * @param chain the chain to set
   */
  public void setChain(BlockChainName chain) {
    this.chain = chain;
  }

  /**
   * @return the blocks
   */
  public long getBlocks() {
    return blocks;
  }

  /**
   * @param blocks the blocks to set
   */
  public void setBlocks(long blocks) {
    this.blocks = blocks;
  }

  /**
   * @return the headers
   */
  public long getHeaders() {
    return headers;
  }

  /**
   * @param headers the headers to set
   */
  public void setHeaders(long headers) {
    this.headers = headers;
  }

  /**
   * @return the bestBlockHash
   */
  public String getBestBlockHash() {
    return bestBlockHash;
  }

  /**
   * @param bestBlockHash the bestBlockHash to set
   */
  public void setBestBlockHash(String bestBlockHash) {
    this.bestBlockHash = bestBlockHash;
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
   * @return the verificationProgress
   */
  public BigDecimal getVerificationProgress() {
    return verificationProgress;
  }

  /**
   * @param verificationProgress the verificationProgress to set
   */
  public void setVerificationProgress(BigDecimal verificationProgress) {
    this.verificationProgress = verificationProgress;
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

}
