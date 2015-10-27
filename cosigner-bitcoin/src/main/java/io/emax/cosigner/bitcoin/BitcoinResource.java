package io.emax.cosigner.bitcoin;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;

import io.emax.cosigner.bitcoin.bitcoindrpc.BitcoindRpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;

/**
 * Static connection to a bitcoind RPC server.
 * 
 * @author Tom
 */
public class BitcoinResource {
  private static final Logger logger = LoggerFactory.getLogger(BitcoinResource.class);
  private static BitcoinResource serverResource = new BitcoinResource();
  private BitcoinConfiguration config;
  private JsonRpcHttpClient client;
  private BitcoindRpc bitcoindRpc;

  public static BitcoinResource getResource() {
    return serverResource;
  }

  private BitcoinResource() {
    this.config = new BitcoinConfiguration();
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
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      logger.error(errors.toString());
    }
  }

  public BitcoinResource(BitcoindRpc rpc) {
    this.bitcoindRpc = rpc;
  }

  public void setBitcoindRpc(BitcoindRpc rpc) {
    this.bitcoindRpc = rpc;
  }

  /**
   * Get an RPC object that is connected to a bitcoind node.
   * 
   * @return RPC object
   */
  public BitcoindRpc getBitcoindRpc() {
    if (bitcoindRpc == null) {
      this.bitcoindRpc =
          ProxyUtil.createClientProxy(getClass().getClassLoader(), BitcoindRpc.class, client);
    }

    return this.bitcoindRpc;
  }
}
