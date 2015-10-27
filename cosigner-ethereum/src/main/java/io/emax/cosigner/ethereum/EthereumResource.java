package io.emax.cosigner.ethereum;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;

import io.emax.cosigner.ethereum.gethrpc.EthereumRpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Static connection to a geth RPC server.
 * 
 * @author Tom
 */
public class EthereumResource {
  private static final Logger logger = LoggerFactory.getLogger(EthereumResource.class);
  private static EthereumResource serverResource = new EthereumResource();
  private EthereumConfiguration config;
  private JsonRpcHttpClient client;
  private EthereumRpc ethereumRpc;

  public static EthereumResource getResource() {
    return serverResource;
  }

  private EthereumResource() {
    this.config = new EthereumConfiguration();
    try {
      this.client = new JsonRpcHttpClient(new URL(config.getDaemonConnectionString()));
    } catch (MalformedURLException e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      logger.error(errors.toString());
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
