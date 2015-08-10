package io.emax.heimdal.blockchains.ethereum;

import io.emax.heimdal.blockchains.ethereum.Client.Transactor;

import java.util.Objects;

import junit.framework.TestCase;

import org.junit.Assert;

public class MultiSigWalletFactoryTest extends TestCase {

  private int goEthereumPort = 8545;
  private int cppEthereumPort = 8080;
  private Process process1, process2;

  public void setUp() throws Exception {
    super.setUp();
    process1 = Runtime.getRuntime().exec("geth --nat none --rpc --rpcport " + goEthereumPort);
    // go ethereum doesn't wake up its rpc right away, so spin wait...
    Thread.sleep(4000);
    process2 = Runtime.getRuntime().exec("eth -j --json-rpc-port " + cppEthereumPort);
    Thread.sleep(500);
  }

  public void tearDown() throws Exception {
    if (process1 != null)
      process1.destroy();
    if (process2 != null)
      process2.destroy();
  }

  public void testMakeNewMultiSigWallet() throws Exception {
    Transactor transactor1 = new Transactor("http://localhost:" + goEthereumPort + "/"), transactor2 =
        new Transactor("http://localhost:" + cppEthereumPort + "/");

    Assert.assertTrue("Coinbase returns with a string for monitor1", transactor1.coinbase()
        .getClass() == String.class);
    Assert.assertTrue("Coinbase returns with a string for monitor2", transactor2.coinbase()
        .getClass() == String.class);
    System.out.println(transactor1.coinbase());
    Assert.assertTrue("Coinbase " + transactor1.coinbase() + " for monitor1 is hexadecimal",
        TestUtils.isHexNumber(transactor1.coinbase()));
    Assert.assertTrue("Coinbase " + transactor2.coinbase() + " for monitor2 is hexadecimal",
        TestUtils.isHexNumber(transactor2.coinbase()));
    Assert.assertTrue("Coinbase " + transactor1.coinbase()
        + " for monitor1 is not 0x0000000000000000000000000000000000000000",
        !Objects.equals(transactor1.coinbase(), "0x0000000000000000000000000000000000000000"));
    Assert.assertTrue("Coinbase " + transactor2.coinbase()
        + " for monitor2 is not 0x0000000000000000000000000000000000000000",
        !Objects.equals(transactor2.coinbase(), "0x0000000000000000000000000000000000000000"));
    MultiSigWalletFactory multiSigWalletFactory =
        new MultiSigWalletFactory(transactor1, transactor2, "0xDEADBEEFDEADBEEF");
    Assert.assertTrue("signer0's address is assigned to the cold storage address",
        multiSigWalletFactory.makeNewMultiSigWallet().contract
            .contains("address private signer0 = 0xDEADBEEFDEADBEEF"));
    Assert.assertTrue("monitor1's address is assigned to the first coinbase", multiSigWalletFactory
        .makeNewMultiSigWallet().contract.contains("address private signer1 = "
        + transactor1.coinbase()));
    Assert.assertTrue("monitor2's address is assigned to the second coinbase",
        multiSigWalletFactory.makeNewMultiSigWallet().contract
            .contains("address private signer2 = " + transactor2.coinbase()));
  }
}
