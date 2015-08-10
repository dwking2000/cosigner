package io.emax.heimdal.bitcoin;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;

import io.emax.heimdal.bitcoin.bitcoind.BitcoindRpc;

/**
 * Static connection to a bitcoind RPC server
 * @author Tom
 */
public class BitcoindResource {
	private static BitcoindResource serverResource;
	private CurrencyConfiguration config;
	private JsonRpcHttpClient client;
	private BitcoindRpc bitcoindRpc;
	
	public static BitcoindResource getResource() {
		if(serverResource == null)
			serverResource = new BitcoindResource();
		return serverResource;
	}
	
	private BitcoindResource() {
		this.config = new CurrencyConfiguration();
		try {
			
			// Set up our RPC authentication
			// TODO: Remove the magic
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication("bitcoinrpc", "changeit".toCharArray());
				}
			});
			
			this.client = new JsonRpcHttpClient(
				    new URL(config.getDaemonConnectionString()));
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public BitcoindRpc getBitcoindRpc(){
		if(bitcoindRpc == null)
			this.bitcoindRpc = ProxyUtil.createClientProxy(getClass().getClassLoader(), BitcoindRpc.class, client);
		
		return this.bitcoindRpc;
	}
}
