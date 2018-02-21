package io.emax.cosigner.ethereum.core;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;

import io.emax.cosigner.ethereum.core.gethrpc.EthereumRpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Static connection to a geth RPC server.
 *
 * @author Tom
 */
public class EthereumResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(EthereumResource.class);
  private static final EthereumResource serverResource = new EthereumResource();
  private JsonRpcHttpClient writeClient;
  private JsonRpcHttpClient readClient;
  private EthereumRpc ethereumRpc;

  private static EthereumRpc innerRpc;
  private static LinkedList<Lock> requestLocks = new LinkedList<>();
  private int concurrentRpcRequests = 10;

  public static EthereumResource getResource() {
    return serverResource;
  }

  private EthereumResource() {
    EthereumConfiguration config = new EthereumConfiguration();
    concurrentRpcRequests = config.getMaxNodeConnections();

    try {
      HashMap<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      this.writeClient =
          new JsonRpcHttpClient(new URL(config.getDaemonConnectionString()), headers);
      this.readClient =
          new JsonRpcHttpClient(new URL(config.getDaemonReadConnectionString()), headers);
      readClient.setConnectionTimeoutMillis(600000);
      readClient.setReadTimeoutMillis(600000);
      writeClient.setConnectionTimeoutMillis(600000);
      writeClient.setReadTimeoutMillis(600000);
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
   * Returns an RPC connector that should be pointing to the node. This method is intended to fetch
   * the node we're using for sending transactions.
   *
   * @return RPC connector
   */
  public EthereumRpc getEthWriteRPC() {
    if (ethereumRpc == null) {
      // We're creating multiple reentrant locks to allow a limited number of requests to run at the same time
      requestLocks.clear();
      for (int i = 0; i < concurrentRpcRequests; i++) {
        requestLocks.add(new ReentrantLock());
      }
      innerRpc =
          ProxyUtil.createClientProxy(getClass().getClassLoader(), EthereumRpc.class, writeClient);
      // Proxy wrapped around the jsonrpc4j calls, will only make the actual call to the node
      // when it can obtain a lock. Otherwise it will wait for a small amount of time before
      // trying the next availble lock.
      this.ethereumRpc = (EthereumRpc) Proxy
          .newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EthereumRpc.class},
              (o, method, objects) -> {
                int lockNumber = 0;
                while (true) {
                  if (requestLocks.get(lockNumber).tryLock()) {
                    try {
                      return method.invoke(innerRpc, objects);
                    } catch (Exception e) {
                      LOGGER.debug("Problem invoking RPC call", e);
                      // We don't want the invocation error, we want the node response.
                      LOGGER.debug("Throwing", e.getCause());
                      throw e.getCause();
                    } finally {
                      requestLocks.get(lockNumber).unlock();
                    }
                  } else {
                    lockNumber++;
                    lockNumber %= requestLocks.size();
                    LOGGER.debug(
                        "Failed to obtain EthereumRpc lock, trying the next one: " + lockNumber);
                    Thread.sleep(1);
                  }
                }
              });
    }

    return this.ethereumRpc;
  }

  /**
   * Returns an RPC connector that should be pointing to the node. This method is intended to fetch
   * the node we're using for reading transaction states.
   *
   * @return RPC connector
   */
  public EthereumRpc getEthReadRPC() {
    if (ethereumRpc == null) {
      // We're creating multiple reentrant locks to allow a limited number of requests to run at the same time
      requestLocks.clear();
      for (int i = 0; i < concurrentRpcRequests; i++) {
        requestLocks.add(new ReentrantLock());
      }
      innerRpc =
          ProxyUtil.createClientProxy(getClass().getClassLoader(), EthereumRpc.class, readClient);
      // Proxy wrapped around the jsonrpc4j calls, will only make the actual call to the node
      // when it can obtain a lock. Otherwise it will wait for a small amount of time before
      // trying the next availble lock.
      this.ethereumRpc = (EthereumRpc) Proxy
          .newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EthereumRpc.class},
              (o, method, objects) -> {
                int lockNumber = 0;
                while (true) {
                  if (requestLocks.get(lockNumber).tryLock()) {
                    try {
                      return method.invoke(innerRpc, objects);
                    } catch (Exception e) {
                      LOGGER.debug("Problem invoking RPC call", e);
                      // We don't want the invocation error, we want the node response.
                      LOGGER.debug("Throwing", e.getCause());
                      throw e.getCause();
                    } finally {
                      requestLocks.get(lockNumber).unlock();
                    }
                  } else {
                    lockNumber++;
                    lockNumber %= requestLocks.size();
                    LOGGER.debug(
                        "Failed to obtain bitcoinRpc lock, trying the next one: " + lockNumber);
                    Thread.sleep(1);
                  }
                }
              });
    }

    return this.ethereumRpc;
  }
}
