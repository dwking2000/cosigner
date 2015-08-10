package io.emax.heimdal.bitcoin.bitcoind;

import java.math.BigDecimal;

import com.googlecode.jsonrpc4j.JsonRpcMethod;

/**
 * Utility RPCs
 * 
 * CreateMultiSig: creates a P2SH multi-signature address. EstimateFee: estimates the transaction
 * fee per kilobyte that needs to be paid for a transaction to be included within a certain number
 * of blocks. New in 0.10.0 PENDING: EstimatePriority: estimates the priority that a transaction
 * needs in order to be included within a certain number of blocks as a free high-priority
 * transaction. New in 0.10.0 ValidateAddress: returns information about the given Bitcoin address.
 * PENDING: VerifyMessage: verifies a signed message
 * 
 * @author dquintela
 */
public interface UtilityRpc {
  /**
   * CreateMultiSig
   * 
   * The createmultisig RPC creates a P2SH multi-signature address.
   * 
   * @param nrequired The minimum (m) number of signatures required to spend this m-of-n multisig
   *        script
   * @param keys the full public keys, or addresses for known public keys
   * @return P2SH address and hex-encoded redeem script
   */
  @JsonRpcMethod("createmultisig")
  MultiSig createMultiSig(int nrequired, String[] keys);

  /**
   * EstimateFee
   * 
   * The estimatefee RPC estimates the transaction fee per kilobyte that needs to be paid for a
   * transaction to be included within a certain number of blocks.
   * 
   * @param blocks The maximum number of blocks a transaction should have to wait before it is
   *        predicted to be included in a block
   * @return the fee the transaction needs to pay per kilobyte, If the node doesn’t have enough
   *         information to make an estimate, the value -1 will be returned
   */
  @JsonRpcMethod("estimatefee")
  BigDecimal estimateFee(int blocks);

  /**
   * ValidateAddress
   * 
   * The validateaddress RPC returns information about the given Bitcoin address.
   */
  @JsonRpcMethod("validateaddress")
  AddressValidated validateAddress(String address);
}
