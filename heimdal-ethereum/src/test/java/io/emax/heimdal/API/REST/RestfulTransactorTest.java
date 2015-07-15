package io.emax.heimdal.API.REST;

import com.google.gson.Gson;

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

import io.emax.heimdal.API.REST.RestfulTransactor;
import io.emax.heimdal.common.Balance;
import io.emax.heimdal.common.BalanceConfirmation;
import io.emax.heimdal.common.Transfer;
import io.emax.heimdal.common.Ledger.Transactor;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class MockTestTransactor implements Transactor {

    private String currency = "TEST";

    @Override
    public String getCurrency() {
        return currency;
    }

    public Balance getBalance() throws Exception {
        return new Balance(
                "0xDEADBEEFDEADBEEF",
                "5",
                currency,
                new BalanceConfirmation(Long.valueOf(1000), (long) 1000, "IMAFRIENDLYBLOCKCHAIN"),
                null,
                Boolean.TRUE);
    }

    public List<Balance> transfer(List<Transfer> transfers) {
        return Stream.concat(
                transfers.stream()
                        .map(transfer -> new Balance(transfer.from, BigInteger.valueOf(100).subtract(new BigInteger(transfer.amount, 16)).toString(16), currency, null, null, Boolean.FALSE)),
                transfers.stream()
                        .map(transfer -> new Balance(transfer.to, BigInteger.valueOf(100).add(new BigInteger(transfer.amount, 16)).toString(16), currency, null, null, Boolean.FALSE))
        )
                .collect(Collectors.toList());
    }

    public Balance newAccount() {
        return new Balance("0xDEADBEEFDEADBEEF777", BigInteger.ZERO.toString(16), currency, null, null, Boolean.TRUE);
    }

    public Balance closeAccount(String account, String destinationAccountForRemainingBalance) {
        return new Balance(destinationAccountForRemainingBalance, BigInteger.valueOf(1000).toString(16), currency, null, null, Boolean.TRUE);
    }
}

public class RestfulTransactorTest {
    Component component;
    Transactor transactor;
    final int port = 8182;

    @Before
    public void setUp() throws Exception {
        transactor = new MockTestTransactor();
        Application application = new RestfulTransactor(transactor).getRestletApplication();
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
        String expected = transactor.getCurrency();
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet("http://localhost:" + port + "/TEST/getCurrency");
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
            String out = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            String actual = new Gson().fromJson(out, String.class);
            Assert.assertEquals(expected, actual);
        }
    }

    @Test
    public void testTransfers() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            List<Transfer> transfers = Arrays.asList(
                    new Transfer("to1", "from1", BigInteger.valueOf(7).toString(16), "TEST", null),
                    new Transfer("to2", "from2", BigInteger.valueOf(20).toString(16), "TEST", null)
            );
            StringEntity httpPostParameters = new StringEntity(new Gson().toJson(transfers));
            HttpPost httpPost = new HttpPost("http://localhost:" + port + "/TEST/transfer");
            httpPost.addHeader("content-type", "application/json");
            httpPost.setEntity(httpPostParameters);
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            String out = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            Balance[] actual = new Gson().fromJson(out, Balance[].class);
            List<Balance> expectedBalancesList = transactor.transfer(transfers);
            Balance[] expected = expectedBalancesList.toArray(new Balance[expectedBalancesList.size()]);
            Assert.assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testGetNewAccount() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet("http://localhost:" + port + "/TEST/newAccount");
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
            Balance expected = transactor.newAccount();
            String out = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            Balance actual = new Gson().fromJson(out, Balance.class);
            Assert.assertEquals(expected, actual);
        }
    }

    @Test
    public void testPostNewAccount() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost httpPost = new HttpPost("http://localhost:" + port + "/TEST/newAccount");
            httpPost.addHeader("content-type", "application/json");
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            Balance expected = transactor.newAccount();
            String out = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            Balance actual = new Gson().fromJson(out, Balance.class);
            Assert.assertEquals(expected, actual);
        }
    }

    @Test
    public void testPostCloseAccount() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            Map<String, String> args = new HashMap<>();
            args.put("account", "foo");
            args.put("destinationAccountForRemainingBalance", "bar");

            StringEntity httpPostParameters = new StringEntity(new Gson().toJson(args));
            HttpPost httpPost = new HttpPost("http://localhost:" + port + "/TEST/closeAccount");
            httpPost.addHeader("content-type", "application/json");
            httpPost.setEntity(httpPostParameters);
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            Balance expected = transactor.closeAccount("foo", "bar");
            String out = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            Balance actual = new Gson().fromJson(out, Balance.class);
            Assert.assertEquals(expected, actual);
        }
    }
}