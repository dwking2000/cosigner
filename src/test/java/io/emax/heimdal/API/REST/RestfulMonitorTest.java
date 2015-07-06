package io.emax.heimdal.API.REST;

import com.google.gson.Gson;
import com.sun.org.apache.xpath.internal.operations.Bool;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.data.Protocol;

import io.emax.heimdal.API.REST.RestfulMonitor;
import io.emax.heimdal.API.REST.Responses.ServerError;
import io.emax.heimdal.common.Balance;
import io.emax.heimdal.common.BalanceConfirmation;
import io.emax.heimdal.common.Ledger.Monitor;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class MockTestMonitor implements Monitor {

    private String currency = "TEST";

    @Override
    public String getCurrency() {
        return currency;
    }

    @Override
    public List<Balance> monitor(List<String> accounts) {
        return accounts.stream().map((s) -> new Balance(
                s,
                "5",
                currency,
                new BalanceConfirmation(Long.valueOf(1000), (long) 1000, "IMAFRIENDLYBLOCKCHAIN"),
                null,
                Boolean.TRUE)).collect(Collectors.toList());
    }

    public List<Balance> getBalances() {
        return Collections.singletonList(
                new Balance(
                        "0xDEADBEEF",
                        "5",
                        currency,
                        new BalanceConfirmation(Long.valueOf(1000), (long) 1000, "IMAFRIENDLYBLOCKCHAIN"),
                        null,
                        Boolean.FALSE)
        );
    }

    public List<Balance> getBalances(List<String> accounts) {
        return accounts.stream().map((s) -> new Balance(
                s,
                "5",
                currency,
                new BalanceConfirmation(Long.valueOf(1000), (long) 1000, "IMAFRIENDLYBLOCKCHAIN"),
                null,
                Boolean.TRUE)).collect(Collectors.toList());
    }
}

public class RestfulMonitorTest {
    Component component;
    MockTestMonitor monitor;
    final int port = 8182;

    @Before
    public void setUp() throws Exception {
        monitor = new MockTestMonitor();
        Application application = new RestfulMonitor(monitor).getRestletApplication();
        component = new Component();
        component.getServers().add(Protocol.HTTP, port);
        component.getDefaultHost().attach("/TEST", application);
        component.start();
    }

    @After
    public void tearDown() throws Exception {
        component.stop();
    }

    @Test
    public void testGetCurrency() throws Exception {
        String expected = monitor.getCurrency();
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet("http://localhost:" + port + "/TEST/getCurrency");
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
            String out = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            String actual = new Gson().fromJson(out, String.class);
            Assert.assertEquals(expected, actual);
        }
    }

    @Test
    public void testGetGetBalances() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet("http://localhost:" + port + "/TEST/getBalances");
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
            List<Balance> expectedBalancesList = monitor.getBalances();
            Balance[] expected = expectedBalancesList.toArray(new Balance[expectedBalancesList.size()]);
            String out = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            Balance[] actual = new Gson().fromJson(out, Balance[].class);
            Assert.assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testPostGetBalances() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost httpPost = new HttpPost("http://localhost:" + port + "/TEST/getBalances");
            httpPost.addHeader("content-type", "application/json");
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            List<Balance> expectedBalancesList = monitor.getBalances();
            Balance[] expected = expectedBalancesList.toArray(new Balance[expectedBalancesList.size()]);
            String out = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            Balance[] actual = new Gson().fromJson(out, Balance[].class);
            Assert.assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testPostGetBalancesSadPathThrowsJsonSyntaxExceptionOnResponseParse() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost httpPost = new HttpPost("http://localhost:" + port + "/TEST/getBalances");
            httpPost.addHeader("content-type", "application/json");
            String expectedErroringInput = "\"foop\"";
            httpPost.setEntity(new StringEntity(expectedErroringInput));
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity entity = httpResponse.getEntity();
            String out = EntityUtils.toString(entity, "UTF-8");
            ServerError serverError = new Gson().fromJson(out, ServerError.class);
            String actualErroringInput = serverError.input;
            Assert.assertEquals(expectedErroringInput, actualErroringInput);
        }
    }

    @Test
    public void testPostGetBalancesWithAccounts() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            List<String> accounts = Arrays.asList("foo", "bar", "baz");
            StringEntity httpPostParameters = new StringEntity(new Gson().toJson(accounts));
            HttpPost httpPost = new HttpPost("http://localhost:" + port + "/TEST/getBalances");
            httpPost.addHeader("content-type", "application/json");
            httpPost.setEntity(httpPostParameters);
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            List<Balance> expectedBalancesList = monitor.getBalances(accounts);
            Balance[] expected = expectedBalancesList.toArray(new Balance[expectedBalancesList.size()]);
            String out = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            Balance[] actual = new Gson().fromJson(out, Balance[].class);
            Assert.assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testPostMonitor() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            List<String> accounts = Arrays.asList("foo", "bar", "baz");
            StringEntity httpPostParameters = new StringEntity(new Gson().toJson(accounts));
            HttpPost httpPost = new HttpPost("http://localhost:" + port + "/TEST/monitor");
            httpPost.addHeader("content-type", "application/json");
            httpPost.setEntity(httpPostParameters);
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            List<Balance> expectedMonitoredAccounts = monitor.monitor(accounts);
            Balance[] expected = expectedMonitoredAccounts.toArray(new Balance[expectedMonitoredAccounts.size()]);
            String out = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            Balance[] actual = new Gson().fromJson(out, Balance[].class);
            Assert.assertArrayEquals(expected, actual);
        }
    }

}