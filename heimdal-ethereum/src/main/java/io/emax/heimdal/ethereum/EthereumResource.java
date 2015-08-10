package io.emax.heimdal.ethereum;

import java.net.MalformedURLException;
import java.net.URL;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;

import io.emax.heimdal.ethereum.gethrpc.EthereumRpc;

/**
 * Static connection to a bitcoind RPC server
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

      // Set up our RPC authentication
      // TODO: Remove the magic
      //Authenticator.setDefault(new Authenticator() {
      //  protected PasswordAuthentication getPasswordAuthentication() {
      //    return new PasswordAuthentication("bitcoinrpc", "changeit".toCharArray());
      //  }
      //});

      this.client = new JsonRpcHttpClient(new URL(config.getDaemonConnectionString()));

    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public EthereumRpc getBitcoindRpc() {
    if (ethereumRpc == null)
      this.ethereumRpc =
          ProxyUtil.createClientProxy(getClass().getClassLoader(), EthereumRpc.class, client);

    return this.ethereumRpc;
  }
}
