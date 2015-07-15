package io.emax.heimdal.blockchains.ethereum.Client;

import static java.util.Arrays.asList;
import io.emax.heimdal.blockchains.JSONRPC2;
import io.emax.heimdal.blockchains.ethereum.Client.Parameters.Balance;
import io.emax.heimdal.common.BalanceConfirmation;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;

public class Monitor implements io.emax.heimdal.common.Ledger.Monitor {

  final public URI uri;

  private Timer monitorUpdate;
  private List<io.emax.heimdal.common.Balance> monitoredBalances = Arrays.asList();
  private List<String> monitoredAccounts = Arrays.asList();
  
  final private int confirmationsRequired;
  
  /**
   * Creates a new RestfulMonitor
   *
   * @param url URL of the Ethereum signer RPC service
   * @throws URISyntaxException
   */
  public Monitor(@NotNull String url) throws URISyntaxException {
    uri = new URI(url);
    confirmationsRequired = 10;
    monitorUpdate.schedule(updateMonitoredBalances, 15000, 15000); // TODO - no magic
  }

  // TODO: Balances need to have a number of confirmations associated with them
  /**
   * Get the balance as a hexadecimal string for particular address on Ethereum
   *
   * See https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getbalance
   *
   * @param address A string containing the hexadecimal address
   * @param tag Either "LATEST", "EARLIEST", or "PENDING"
   * @return A string with the quantity in the balanceAsHex
   * @throws IOException
   */
  public String balanceAsHex(String address, Balance tag)
          throws IOException {

    List<String> params = asList(
            address,
            tag.toString().toLowerCase()
    );
    return new Gson()
            .fromJson(JSONRPC2.Call(uri, "eth_getBalance", params), Properties.class)
            .getProperty("result");
  }

  public String balanceAsHex(String address)
          throws IOException {
    return balanceAsHex(address, Balance.LATEST);
  }

  @Override
  public String getCurrency() {
    return "ETH"; // Likely needs to be changed
  }

  TimerTask updateMonitoredBalances = new TimerTask() {
    @Override
    public void run() {
      try {
        monitoredBalances = getBalances(monitoredAccounts);        
      } catch (Exception ex) {
        Logger.getLogger(Monitor.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  };
  
  @Override
  public List<io.emax.heimdal.common.Balance> monitor(List<String> accounts) throws Exception {
    // Allow adding/removing in the future
    monitoredAccounts = accounts;
    monitoredBalances = getBalances(accounts);    
    
    return monitoredBalances;
  }

  @Override
  public List<io.emax.heimdal.common.Balance> getBalances() throws Exception {
    return monitoredBalances;
  }
 
  @Override
  public List<io.emax.heimdal.common.Balance> getBalances(List<String> accounts) throws Exception {
    // Blocking 
    // Ethereum doesn't seem to have an 'inputs' concept like bitcoin, 
    //  We can't easily say that any given input is X confirmations old.
    //  Tying an account input to a specfic block would require manually rebuilding the state
    //  So... Get the hash for the latest and for the (latest - confirmation) blocks to build a balance object for now
    
    // Get the latest block num
    List<String> params = Arrays.asList();
    String latestBlockNumber = new Gson()
            .fromJson(JSONRPC2.Call(uri, "eth_blockNumber", params), Properties.class)
            .getProperty("result");
    
    // Get the latest block hash 
    params = Arrays.asList(latestBlockNumber, "false");
    String latestBlockJson = new Gson()
            .fromJson(JSONRPC2.Call(uri, "eth_getBlockByNumber", params), Properties.class)
            .getProperty("result"); 
    String latestBlockTimestamp = new Gson().fromJson(latestBlockJson, Properties.class).getProperty("timestamp");
    String latestBlockHash = new Gson().fromJson(latestBlockJson, Properties.class).getProperty("hash");
    
    // Get the confirmed block number and hash
    String confirmedBlockNumber = Long.toHexString(Long.parseLong(latestBlockNumber, 16) - confirmationsRequired);
    // And Hash
    params = Arrays.asList(confirmedBlockNumber, "false");
    String confirmedBlockJson = new Gson()
            .fromJson(JSONRPC2.Call(uri, "eth_getBlockByNumber", params), Properties.class)
            .getProperty("result"); 
    String confirmedBlockTimestamp = new Gson().fromJson(confirmedBlockJson, Properties.class).getProperty("timestamp");
    String confirmedBlockHash = new Gson().fromJson(confirmedBlockJson, Properties.class).getProperty("hash");
    
    // foreach account, request the balance at the <latest> - <confirmations> block
    List<io.emax.heimdal.common.Balance> balances = Arrays.asList();
    io.emax.heimdal.common.BalanceConfirmation confirmedBlock = new BalanceConfirmation(Long.parseLong(confirmedBlockTimestamp, 16), Long.parseLong(confirmedBlockNumber, 16), confirmedBlockHash);
    io.emax.heimdal.common.BalanceConfirmation latestBlock = new BalanceConfirmation(Long.parseLong(latestBlockTimestamp, 16), Long.parseLong(latestBlockNumber, 16), latestBlockHash);
    for (String account : accounts) {
      params = Arrays.asList(account, confirmedBlockNumber);
      String accountBalanceValue = new Gson()
              .fromJson(JSONRPC2.Call(uri, "eth_getBalance", params), Properties.class)
              .getProperty("result");
      io.emax.heimdal.common.Balance accountBalance = new io.emax.heimdal.common.Balance(account, accountBalanceValue, this.getCurrency(), confirmedBlock, latestBlock, Boolean.FALSE);
      balances.add(accountBalance);
    }
    return balances;
  }
}
