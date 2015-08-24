package io.emax.heimdal.bitcoin.bitcoindrpc;

/**
 * Bitcoind RPC Interface
 * 
 * @author dquintela
 */
public interface BitcoindRpc extends BlockChainRpc, ControlRpc, GeneratingRpc, MiningRpc,
    NetworkRpc, RawTransactionRpc, UtilityRpc, WalletRpc {
}
