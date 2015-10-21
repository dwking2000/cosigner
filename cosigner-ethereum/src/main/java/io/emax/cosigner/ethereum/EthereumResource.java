package io.emax.cosigner.ethereum;

import java.net.MalformedURLException;
import java.net.URL;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;

import io.emax.cosigner.ethereum.gethrpc.EthereumRpc;

/**
 * Static connection to a geth RPC server
 * 
 * @author Tom
 */
public class EthereumResource {
  private static EthereumResource serverResource;
  private CurrencyConfiguration config;
  private JsonRpcHttpClient client;
  private EthereumRpc ethereumRpc;

  public static EthereumResource getResource() {
    if (serverResource == null)
      serverResource = new EthereumResource();
    return serverResource;
  }

  private EthereumResource() {
    this.config = new CurrencyConfiguration();
    try {
      this.client = new JsonRpcHttpClient(new URL(config.getDaemonConnectionString()));
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

  public EthereumResource(EthereumRpc rpc) {
    this.ethereumRpc = rpc;
  }
  
  public void setEthereumRpc(EthereumRpc rpc) {
    this.ethereumRpc = rpc;
  }

  public EthereumRpc getGethRpc() {
    if (ethereumRpc == null)
      this.ethereumRpc =
          ProxyUtil.createClientProxy(getClass().getClassLoader(), EthereumRpc.class, client);

    return this.ethereumRpc;
  }
}
