package io.emax.heimdal.blockchains.ethereum;

import io.emax.heimdal.blockchains.ethereum.Client.Monitor;
import io.emax.heimdal.blockchains.ethereum.Client.Transactor;
import junit.framework.TestCase;

import org.junit.Assert;

import java.math.BigInteger;
import java.util.Objects;


public class TransactorTest extends TestCase {

    private int defaultPort = 8545;
    private Process process;

    public void setUp() throws Exception {
        super.setUp();
        // Account password is "testingtesting123"
        process = Runtime.getRuntime().exec("geth --nat none --rpc --rpcport " + defaultPort);
        // go ethereum doesn't wake up its rpc right away, so spin wait...
        Thread.sleep(4000);
    }

    public void tearDown() throws Exception {
        if (process != null) {
            process.destroy();
        }
    }

    public void testCoinbase() throws Exception {
        Monitor monitor = new Monitor("http://localhost:" + defaultPort + "/");
        Transactor transactor = new Transactor("http://localhost:" + defaultPort + "/");
        Assert.assertTrue(
                "Coinbase returns with a string",
                transactor.coinbase().getClass() == String.class);
        Assert.assertTrue(
                "Coinbase " + transactor.coinbase() + " is hexadecimal",
                TestUtils.isHexNumber(transactor.coinbase()));

        System.out.println("Coinbase: " + transactor.coinbase());
        Assert.assertTrue(
                "Coinbase " + transactor.coinbase() + " is not 0x0000000000000000000000000000000000000000",
                !Objects.equals(transactor.coinbase(), "0x0000000000000000000000000000000000000000")
        );
        System.out.println("gasPriceAsHex: " + transactor.gasPriceAsHex());
        Assert.assertTrue(
                "gasPriceAsHex " + transactor.gasPriceAsHex() + " is hexadecimal",
                TestUtils.isHexNumber(transactor.gasPriceAsHex()));
        System.out.println("balanceAsHex: " + monitor.balanceAsHex(transactor.coinbase()));
        Assert.assertTrue(
                "balanceAsHex " + monitor.balanceAsHex(transactor.coinbase()) + " is non-zero",
                !BigInteger.ZERO.equals(new BigInteger(monitor.balanceAsHex(transactor.coinbase()).substring(2), 16)));
    }
}