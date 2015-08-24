package io.emax.heimdal.bitcoin.bitcoindrpc;

/**
 * Mining RPCs
 * 
 * PENDING: GetBlockTemplate: gets a block template or proposal for use with mining software.
 * PENDING: GetMiningInfo: returns various mining-related information. Updated in master PENDING:
 * GetNetworkHashPS: returns the estimated current or historical network hashes per second based on
 * the last n blocks. PENDING: PrioritiseTransaction: adds virtual priority or fee to a transaction,
 * allowing it to be accepted into blocks mined by this node (or miners which use this node) with a
 * lower priority or fee. (It can also remove virtual priority or fee, requiring the transaction
 * have a higher priority or fee to be accepted into a locally-mined block.) New in 0.10.0 PENDING:
 * SubmitBlock: accepts a block, verifies it is a valid addition to the block chain, and broadcasts
 * it to the network. Extra parameters are ignored by Bitcoin Core but may be used by mining pools
 * or other programs.
 * 
 * @author dquintela
 */
public interface MiningRpc {
}
