package io.emax.cosigner.bitcoin;

import io.emax.cosigner.bitcoin.stubrpc.BitcoinTestRpc;

import org.junit.Test;

import junit.framework.TestCase;
import rx.Subscription;

import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MonitorTest extends TestCase {
  private BitcoinWallet wallet;
  private BitcoinMonitor monitor;
  private String userKey;
  private int balanceCounter;

  @Override
  public void setUp() {
    BitcoinResource.getResource().setBitcoindRpc(new BitcoinTestRpc());
    wallet = new BitcoinWallet();
    monitor = new BitcoinMonitor();
    userKey = "deadbeef";
    balanceCounter = 0;
  }

  @Test
  public void testMonitor() throws InterruptedException {
    System.out.println("");
    System.out.println("Simple monitor test");
    // Get addresses
    String singleAddress = wallet.createAddress(userKey);
    System.out.println("Single Address Test: " + singleAddress);
    String multiAddress = wallet.getMultiSigAddress(Arrays.asList(singleAddress), userKey);
    System.out.println("Multi Address Test: " + multiAddress);
    monitor.addAddresses(Arrays.asList(multiAddress));

    singleAddress = wallet.createAddress(userKey);
    System.out.println("Single Address Test: " + singleAddress);
    multiAddress = wallet.getMultiSigAddress(Arrays.asList(singleAddress), userKey);
    System.out.println("Multi Address Test: " + multiAddress);
    monitor.addAddresses(Arrays.asList(multiAddress));

    System.out.println("Waiting for balance updates (2 minutes)... ");
    System.out.println("Time: " + new Date());

    Subscription monitorSub = monitor.getObservableBalances().subscribe(addresses -> {
      System.out.println("Time: " + new Date());
      addresses.forEach((address, balance) -> {
        balanceCounter++;
        System.out.println("Address: " + address + " Balance: " + balance);
      });
    });

    Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        System.out.println("Unsubscribing from monitor. Test should complete within 10 seconds.");
        System.out.println("Time: " + new Date());
        monitorSub.unsubscribe();
      }
    }, 2 * 60 * 1000);

    while (!monitorSub.isUnsubscribed()) {
      Thread.sleep(10 * 1000);
    }

    assertTrue(balanceCounter > 0);
  }
}
