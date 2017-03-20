package io.emax.cosigner.ethereum.core;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;

import io.emax.cosigner.ethereum.core.gethrpc.EthereumRpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * Static connection to a geth RPC server.
 *
 * @author Tom
 */
public class EthereumResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(EthereumResource.class);
  private static final EthereumResource serverResource = new EthereumResource();
  private JsonRpcHttpClient client;
  private EthereumRpc ethereumRpc;

  public static EthereumResource getResource() {
    return serverResource;
  }

  private EthereumResource() {
    EthereumConfiguration config = new EthereumConfiguration();
    try {
      HashMap<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      this.client = new JsonRpcHttpClient(new URL(config.getDaemonConnectionString()), headers);
    } catch (MalformedURLException e) {
      LOGGER.error(null, e);
    }
  }

  public EthereumResource(EthereumRpc rpc) {
    this.ethereumRpc = rpc;
  }

  public void setEthereumRpc(EthereumRpc rpc) {
    this.ethereumRpc = rpc;
  }

  /**
   * Returns an RPC connector that should be pointing to the geth node.
   *
   * @return RPC connector
   */
  public EthereumRpc getGethRpc() {
    if (ethereumRpc == null) {
      this.ethereumRpc =
          ProxyUtil.createClientProxy(getClass().getClassLoader(), EthereumRpc.class, client);
    }

    return this.ethereumRpc;
  }
}
