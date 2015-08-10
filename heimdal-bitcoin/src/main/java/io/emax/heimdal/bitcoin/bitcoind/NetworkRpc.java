package io.emax.heimdal.bitcoin.bitcoind;

import com.googlecode.jsonrpc4j.JsonRpcMethod;

/**
 * Network RPCs
 * 
 * AddNode: attempts to add or remove a node from the addnode list, or to try a connection to a node
 * once. PENDING: GetAddedNodeInfo: returns information about the given added node, or all added
 * nodes (except onetry nodes). Only nodes which have been manually added using the addnode RPC will
 * have their information displayed. PENDING: GetConnectionCount: returns the number of connections
 * to other nodes. PENDING: GetNetTotals: returns information about network traffic, including bytes
 * in, bytes out, and the current time. PENDING: GetNetworkInfo: returns information about the
 * node’s connection to the network. New in 0.9.2, Updated in 0.10.0 PENDING: GetPeerInfo: returns
 * data about each connected network node. Updated in 0.10.0 PENDING: Ping: sends a P2P ping message
 * to all connected nodes to measure ping time. Results are provided by the getpeerinfo RPC pingtime
 * and pingwait fields as decimal seconds. The P2P ping message is handled in a queue with all other
 * commands, so it measures processing backlog, not just network ping
 * 
 * @author dquintela
 */
public interface NetworkRpc {
  public enum AddNodeCommand {
    /**
     * To add a node to the addnode list. This will not connect immediately if the outgoing
     * connection slots are full
     */
    add, /**
          * To remove a node from the list. If currently connected, this will disconnect immediately
          */
    remove, /**
             * To immediately attempt connection to the node even if the outgoing connection slots
             * are full; this will only attempt the connection once
             */
    onetry
  }

  /**
   * AddNode
   * 
   * The addnode RPC attempts to add or remove a node from the addnode list, or to try a connection
   * to a node once.
   * 
   * @param node The node to add as a string in the form of <IP address>:<port>. The IP address may
   *        be a hostname resolvable through DNS, an IPv4 address, an IPv4-as-IPv6 address, or an
   *        IPv6 address
   * @param command whether to add or remove the node, or to try only once to connect
   * 
   * @return
   */
  @JsonRpcMethod("addnode")
  void addNode(String node, AddNodeCommand command);
}
