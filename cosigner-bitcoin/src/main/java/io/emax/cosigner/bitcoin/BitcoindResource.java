package io.emax.cosigner.bitcoin;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;

import io.emax.cosigner.bitcoin.bitcoindrpc.BitcoindRpc;

/**
 * Static connection to a bitcoind RPC server
 * 
 * @author Tom
 */
public class BitcoindResource {
  private static BitcoindResource serverResource;
  private CurrencyConfiguration config;
  private JsonRpcHttpClient client;
  private BitcoindRpc bitcoindRpc;

  public static BitcoindResource getResource() {
    if (serverResource == null)
      serverResource = new BitcoindResource();
    return serverResource;
  }

  private BitcoindResource() {
    this.config = new CurrencyConfiguration();
    try {

      // Set up our RPC authentication
      Authenticator.setDefault(new Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(config.getDaemonUser(),
              config.getDaemonPassword().toCharArray());
        }
      });

      this.client = new JsonRpcHttpClient(new URL(config.getDaemonConnectionString()));

    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

  public BitcoindResource(BitcoindRpc rpc) {
    this.bitcoindRpc = rpc;
  }
  
  public void setBitcoindRpc(BitcoindRpc rpc) {
    this.bitcoindRpc = rpc;
  }

  public BitcoindRpc getBitcoindRpc() {
    if (bitcoindRpc == null)
      this.bitcoindRpc =
          ProxyUtil.createClientProxy(getClass().getClassLoader(), BitcoindRpc.class, client);

    return this.bitcoindRpc;
  }
}
