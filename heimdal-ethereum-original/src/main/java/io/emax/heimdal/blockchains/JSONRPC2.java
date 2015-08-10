package io.emax.heimdal.blockchains;

import io.emax.heimdal.internal.Pool;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;

/**
 * POJO for GSON serialization to JSON for talking to JSONRPC2 interfaces
 *
 * @param <T>
 */
public class JSONRPC2<T> {
  public final String jsonrpc = "2.0";
  public String method;
  public List<T> params = new ArrayList<>();
  public final String id = UUID.randomUUID().toString();

  public JSONRPC2(@NotNull String method, List<T> params) {
    this.method = method;
    this.params = params == null ? new ArrayList<>() : params;
  }

  /**
   * Make a call to the RestfulMonitor RPC with a list of Map parameters
   *
   * @param method the method to be called
   * @param params A list of parameters for said method; in practice either a list of strings or a
   *        singleton list containing a Map
   * @return A string containing the RPC output (which is a JSON)
   * @throws IOException
   */
  public static String Call(@NotNull URI uri, @NotNull String method, List<?> params)
      throws IOException {

    HttpPost httpPost = new HttpPost(uri);
    StringEntity httpPostParameters = new StringEntity(new JSONRPC2<>(method, params).toString());
    httpPost.addHeader("content-type", "application/json");
    httpPost.setEntity(httpPostParameters);
    CloseableHttpResponse httpResponse = Pool.httpClient.execute(httpPost);
    String output = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
    httpResponse.close();
    return output;
  }

  public static String Call(@NotNull URI uri, @NotNull String method) throws IOException {
    return Call(uri, method, null);
  }

  @Override
  public String toString() {
    return (new Gson()).toJson(this);
  }
}
