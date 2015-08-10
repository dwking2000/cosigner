package io.emax.heimdal.bitcoin.bitcoind;

/**
 * Control RPCs
 * 
 * PENDING: GetInfo: prints various information about the node and the network. Updated in 0.10.0, Deprecated
 * Help: lists all available public RPC commands, or gets help for the specified RPC. Commands which are unavailable will not be listed, such as wallet RPCs if wallet support is disabled.
 * PENDING: Stop: safely shuts down the Bitcoin Core server.
 * 
 * @author dquintela
 */
public interface ControlRpc {
    String help(String command);
}
