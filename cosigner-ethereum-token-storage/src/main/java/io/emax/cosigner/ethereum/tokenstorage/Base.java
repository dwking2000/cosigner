package io.emax.cosigner.ethereum.tokenstorage;

import io.emax.cosigner.ethereum.core.EthereumResource;
import io.emax.cosigner.ethereum.core.gethrpc.EthereumRpc;

/**
 * Common definitions that every Token class may need.
 */
public class Base {
  // Testnet specific variables for Ethereum
  static final String TESTNET_VERSION = "2";
  static final long TESTNET_BASE_ROUNDS = (long) Math.pow(2, 20);

  // Contract type IDs
  static final String ADMIN = "admin";
  static final String TOKEN = "tokenstorage";
  static final String STORAGE = "storage";

  // RPC
  public static final EthereumRpc ethereumWriteRpc = EthereumResource.getResource().getEthWriteRPC();
  public static final EthereumRpc ethereumReadRpc = EthereumResource.getResource().getEthReadRPC();
}
