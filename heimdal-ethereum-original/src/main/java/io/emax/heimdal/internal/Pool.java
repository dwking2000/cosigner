package io.emax.heimdal.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class Pool {
  final public static ExecutorService executorService = Executors.newCachedThreadPool();
  final public static CloseableHttpClient httpClient;

  static {
    final PoolingHttpClientConnectionManager connectionManager =
        new PoolingHttpClientConnectionManager();
    // Increase max total connection to 200
    connectionManager.setMaxTotal(200);
    // Increase default max connection per route to 20
    connectionManager.setDefaultMaxPerRoute(20);
    httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();

  }
}
