package io.emax.heimdal.bitcoin.bitcoind;

/**
 * Bitcoind RPC Interface
 * 
 * @author dquintela
 */
public interface BitcoindRpc extends BlockChainRpc, ControlRpc, GeneratingRpc, MiningRpc,
    NetworkRpc, RawTransactionRpc, UtilityRpc, WalletRpc {
}
