package io.emax.cosigner.admin;

import io.emax.cosigner.api.core.Server;
import io.emax.cosigner.common.Json;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminConnector {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdminConnector.class);
  private ClientConfiguration config = new ClientConfiguration();
  private HttpClient httpClient = new HttpClient();

  /**
   * Constructor for the connector.
   *
   * <p>Sets up TLS if it's configured.
   */
  public AdminConnector() {
    if (config.useTls()) {
      SslContextFactory sslContext = new SslContextFactory(config.getTlsKeystore());
      sslContext.setKeyStorePassword(config.getTlsKeystorePassword());
      sslContext.setTrustStorePath(config.getTlsKeystore());
      sslContext.setTrustStorePassword(config.getTlsKeystorePassword());
      sslContext.setCertAlias(config.getTlsCertAlias());
      httpClient = new HttpClient(sslContext);
    }
  }

  private String restPostRequest(String endpoint, String content) {
    try {
      LOGGER.debug("Sending POST request to: " + config.getRsServerUrl() + endpoint);
      httpClient.start();
      Request request = httpClient.newRequest(config.getRsServerUrl() + endpoint);
      request = request.method(HttpMethod.POST);
      request = request.content(new StringContentProvider(content, "UTF-8"));
      ContentResponse response = request.send();
      httpClient.stop();
      LOGGER.debug("Got response: " + response.getContentAsString());
      return response.getContentAsString();
    } catch (Exception e) {
      LOGGER.error(null, e);
      return "";
    }
  }

  private String restGetRequest(String endpoint) {
    try {
      LOGGER.debug("Sending GET request to: " + config.getRsServerUrl() + endpoint);
      httpClient.start();
      Request request = httpClient.newRequest(config.getRsServerUrl() + endpoint);
      request = request.method(HttpMethod.GET);
      ContentResponse response = request.send();
      httpClient.stop();
      LOGGER.debug("Got response: " + response.getContentAsString());
      return response.getContentAsString();
    } catch (Exception e) {
      LOGGER.error(null, e);
      return "";
    }
  }

  /**
   * List nodes in the cluster.
   */
  public String listNodes() {
    return restGetRequest("/admin/ListNodes");
  }

  public String addNode(Server server) {
    String serverString = Json.stringifyObject(Server.class, server);
    return restPostRequest("/admin/AddNode", serverString);
  }

  public String getClusterKey() {
    return restGetRequest("/admin/GetClusterKey");
  }

  public void setClusterKey(String key) {
    restPostRequest("/admin/SetClusterKey", key);
  }

  public void setServerLocation(String location) {
    restPostRequest("/admin/SetLocation", location);
  }
}
