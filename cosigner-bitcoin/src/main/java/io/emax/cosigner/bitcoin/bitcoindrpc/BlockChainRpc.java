package io.emax.cosigner.bitcoin.bitcoindrpc;

import com.googlecode.jsonrpc4j.JsonRpcMethod;

/**
 * Block Chain RPCs
 * 
 * PENDING: GetBestBlockHash: returns the header hash of the most recent block on the best block
 * chain. New in 0.9.0 GetBlock: gets a block with a particular header hash from the local block
 * database either as a JSON object or as a serialized block. GetBlockChainInfo: provides
 * information about the current state of the block chain. New in 0.9.2, Updated in 0.10.0
 * GetBlockCount: returns the number of blocks in the local best block chain. GetBlockHash: returns
 * the header hash of a block at the given height in the local best block chain. PENDING:
 * GetChainTips: returns information about the highest-height block (tip) of each local block chain.
 * New in 0.10.0 PENDING: GetDifficulty: returns the proof-of-work difficulty as a multiple of the
 * minimum difficulty. PENDING: GetMemPoolInfo: returns information about the node’s current
 * transaction memory pool. New in 0.10.0 PENDING: GetRawMemPool: returns all transaction
 * identifiers (TXIDs) in the memory pool as a JSON array, or detailed information about each
 * transaction in the memory pool as a JSON object. PENDING: GetTxOut: returns details about a
 * transaction output. Only unspent transaction outputs (UTXOs) are guaranteed to be available.
 * PENDING: GetTxOutSetInfo: returns statistics about the confirmed unspent transaction output
 * (UTXO) set. Note that this call may take some time and that it only counts outputs from confirmed
 * transactions—it does not count outputs from the memory pool. PENDING: VerifyChain: verifies each
 * entry in the local block chain database.
 * 
 * @author dquintela
 */
public interface BlockChainRpc {
  /**
   * GetBlock
   * 
   * The getblock RPC gets a block with a particular header hash from the local block database
   * either as a JSON object or as a serialized block.
   * 
   * @param blockHash The hash of the header of the block to get, encoded as hex in RPC byte order
   */
  @JsonRpcMethod("getblock")
  Block getBlock(String blockHash);

  /**
   * GetBlockChainInfo Added in Bitcoin Core 0.9.2
   * 
   * The getblockchaininfo RPC provides information about the current state of the block chain.
   */
  @JsonRpcMethod("getblockchaininfo")
  BlockChainInfo getblockchaininfo();

  /**
   * GetBlockCount
   * 
   * The getblockcount RPC returns the number of blocks in the local best block chain.
   * 
   * @return The number of blocks in the local best block chain. For a new node with only the
   *         hardcoded genesis block, this number will be 0
   */
  @JsonRpcMethod("getblockcount")
  long getBlockCount();

  /**
   * GetBlockHash
   * 
   * The getblockhash RPC returns the header hash of a block at the given height in the local best
   * block chain.
   * 
   * @param blockheight The height of the block whose header hash should be returned. The height of
   *        the hardcoded genesis block is 0
   */
  @JsonRpcMethod("getblockhash")
  String getBlockHash(long blockHeight);
}
