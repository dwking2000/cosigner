package io.emax.heimdal.blockchains.ethereum;

import io.emax.heimdal.blockchains.ethereum.MultiSigWallet;
import io.emax.heimdal.blockchains.ethereum.MultiSigWalletFactory;
import io.emax.heimdal.blockchains.ethereum.Client.Transactor;
import junit.framework.TestCase;

import org.junit.Assert;


public class CreateAndUploadMultiSigWalletTest extends TestCase {
    private int goEthereumPort = 8545;
    private int cppEthereumPort = 8080;
    private Process process1, process2;

    public void setUp() throws Exception {
        super.setUp();
        process1 = Runtime.getRuntime().exec("geth --nat none --rpc --rpcport " + goEthereumPort);
        // go ethereum doesn't wake up its rpc right away, so spin wait...
        Thread.sleep(4000);
        process2 = Runtime.getRuntime().exec("eth -j --json-rpc-port " + cppEthereumPort);
    }

    public void tearDown() throws Exception {
        if (process1 != null)
            process1.destroy();
        if (process2 != null)
            process2.destroy();
    }

    public void testCreateAndUploadMultiSigWallet() throws Exception {
        Transactor transactor1 = new Transactor("http://localhost:" + goEthereumPort + "/"),
                transactor2 = new Transactor("http://localhost:" + cppEthereumPort + "/");
        MultiSigWalletFactory multiSigWalletFactory =
                new MultiSigWalletFactory(transactor1, transactor2, "0xDEADBEEFDEADBEEF");
        MultiSigWallet multiSigWalletFuture = multiSigWalletFactory.makeNewMultiSigWallet();
        Assert.assertEquals("monitor1 can upload the newly created wallet and returns the address of said contract",
                transactor1.createContract(multiSigWalletFuture),
                multiSigWalletFuture.address);
    }
}
