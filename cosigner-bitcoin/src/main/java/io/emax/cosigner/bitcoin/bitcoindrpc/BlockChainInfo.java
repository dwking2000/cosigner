package io.emax.cosigner.bitcoin.bitcoindrpc;

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
  private String bestblockhash;

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
  private BigDecimal verificationprogress;

  /**
   * The estimated number of block header hashes checked from the genesis block to this block,
   * encoded as big-endian hex
   */
  @JsonProperty("chainwork")
  private String chainwork;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((bestblockhash == null) ? 0 : bestblockhash.hashCode());
    result = prime * result + (int) (blocks ^ (blocks >>> 32));
    result = prime * result + ((chain == null) ? 0 : chain.hashCode());
    result = prime * result + ((chainwork == null) ? 0 : chainwork.hashCode());
    result = prime * result + ((difficulty == null) ? 0 : difficulty.hashCode());
    result = prime * result + (int) (headers ^ (headers >>> 32));
    result = prime * result + ((pruned == null) ? 0 : pruned.hashCode());
    result =
        prime * result + ((verificationprogress == null) ? 0 : verificationprogress.hashCode());
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
    BlockChainInfo other = (BlockChainInfo) obj;
    if (bestblockhash == null) {
      if (other.bestblockhash != null)
        return false;
    } else if (!bestblockhash.equals(other.bestblockhash))
      return false;
    if (blocks != other.blocks)
      return false;
    if (chain != other.chain)
      return false;
    if (chainwork == null) {
      if (other.chainwork != null)
        return false;
    } else if (!chainwork.equals(other.chainwork))
      return false;
    if (difficulty == null) {
      if (other.difficulty != null)
        return false;
    } else if (!difficulty.equals(other.difficulty))
      return false;
    if (headers != other.headers)
      return false;
    if (pruned == null) {
      if (other.pruned != null)
        return false;
    } else if (!pruned.equals(other.pruned))
      return false;
    if (verificationprogress == null) {
      if (other.verificationprogress != null)
        return false;
    } else if (!verificationprogress.equals(other.verificationprogress))
      return false;
    return true;
  }

  @JsonProperty("pruned")
  private String pruned;

  public String getBestblockhash() {
    return bestblockhash;
  }

  public void setBestblockhash(String bestblockhash) {
    this.bestblockhash = bestblockhash;
  }

  public BigDecimal getVerificationprogress() {
    return verificationprogress;
  }

  public void setVerificationprogress(BigDecimal verificationprogress) {
    this.verificationprogress = verificationprogress;
  }

  public String getPruned() {
    return pruned;
  }

  public void setPruned(String pruned) {
    this.pruned = pruned;
  }

  @Override
  public String toString() {
    return "BlockChainInfo [chain=" + chain + ", blocks=" + blocks + ", headers=" + headers
        + ", bestblockhash=" + bestblockhash + ", difficulty=" + difficulty
        + ", verificationprogress=" + verificationprogress + ", chainwork=" + chainwork
        + ", pruned=" + pruned + "]";
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
    return bestblockhash;
  }

  /**
   * @param bestBlockHash the bestBlockHash to set
   */
  public void setBestBlockHash(String bestBlockHash) {
    this.bestblockhash = bestBlockHash;
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
    return verificationprogress;
  }

  /**
   * @param verificationProgress the verificationProgress to set
   */
  public void setVerificationProgress(BigDecimal verificationProgress) {
    this.verificationprogress = verificationProgress;
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
