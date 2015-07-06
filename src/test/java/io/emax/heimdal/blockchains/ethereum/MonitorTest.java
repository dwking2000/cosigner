package io.emax.heimdal.blockchains.ethereum;

import io.emax.heimdal.blockchains.ethereum.Client.Monitor;
import junit.framework.TestCase;

import org.junit.Assert;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MonitorTest extends TestCase {

  private final int defaultPort = 8545;
  private Process process;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // Needs to be fleshed out more before a unit test makes sense, just getting the class in place
    //process = Runtime.getRuntime().exec("geth --nat none --rpc --rpcport " + defaultPort);
    // go ethereum doesn't wake up its rpc right away, so spin wait...
    //Thread.sleep(4000);
  }

  @Override
  public void tearDown() throws Exception {
    if (process != null) {
      process.destroy();
    }
  }

  public void testCoinbase() throws Exception {
    //Monitor monitor = new Monitor("http://localhost:" + defaultPort + "/");
    //List<emax.io.heimdal.common.Balance> balances = monitor.getBalances(Arrays.asList("0x25f62bfccb44c69ab8e9fd3c94aa780e547079d8"));
    //for (emax.io.heimdal.common.Balance balance : balances) {
    //  System.out.println(balance.amount);
    //}

  }
}
