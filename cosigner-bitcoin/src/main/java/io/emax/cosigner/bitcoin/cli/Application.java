package io.emax.cosigner.bitcoin.cli;

import io.emax.cosigner.api.currency.Wallet.Recipient;
import io.emax.cosigner.bitcoin.BitcoinConfiguration;
import io.emax.cosigner.bitcoin.BitcoinMonitor;
import io.emax.cosigner.bitcoin.BitcoinWallet;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Command line option for running the library.
 *
 * @author Tom
 */
public class Application {
  /**
   * Command line interface that provides basic access to the library.
   *
   * @param args Command line arguments, leave blank to see usage.
   * @throws InterruptedException Some library interfaces require time to scan for data, sleep may
   *                              throw this exception.
   */
  public static void main(String[] args) throws InterruptedException {
    if (args.length < 1) {
      System.out.println("Usage: <interfaceMethod> <argument> <argument> ...");
      System.out.println("Available methods:");
      System.out.println("\tgetNewAddress(String accountName)");
      System.out.println("\tgetDeterministicAddresses(String accountName)");
      System.out
          .println("\tgetMultiSigAddress(String address1, String address2, ..., accountName)");
      System.out.println("\tgetBalance(String address)");
      System.out.println("\tcreateTransaction(String fromAddress1, String fromAddress2,"
          + " ..., String toAddress, Decimal amount)");
      System.out.println("\tsignTransaction(String transaction, String address)");
      System.out.println("\tsendTransaction(String transaction)");
      System.out.println("\tgetSignersForTransaction(String transaction)");
      System.out.println("\tmonitor(String address)");
      System.out.println("\tlistTxs(String address, int resultSize, int skipNumber)");
      System.out.println("\tgetTx(String tx-id)");
      return;
    }

    BitcoinWallet wallet = new BitcoinWallet(new BitcoinConfiguration());
    BitcoinMonitor monitor = new BitcoinMonitor(wallet);
    String accountName = "";
    String address = "";
    String transaction = "";
    LinkedList<String> addressList = new LinkedList<>();
    BigDecimal amount = BigDecimal.ZERO;
    int resultSize = 0;
    int skipNumber = 0;
    switch (args[0]) {
      case "getNewAddress":
        if (args.length >= 2) {
          accountName = args[1];
        }
        System.out.println(wallet.createAddress(accountName));
        break;
      case "getDeterministicAddresses":
        if (args.length >= 2) {
          accountName = args[1];
        }
        wallet.getAddresses(accountName).forEach(System.out::println);
        break;
      case "getMultiSigAddress":
        if (args.length >= 2) {
          accountName = args[args.length - 1];
        }
        addressList.addAll(Arrays.asList(args).subList(1, args.length - 1));
        System.out.println(wallet.getMultiSigAddress(addressList, accountName));
        break;
      case "getBalance":
        if (args.length >= 2) {
          accountName = args[1];
        }
        System.out.println(wallet.getBalance(accountName));
        break;
      case "createTransaction":
        if (args.length >= 2) {
          amount = new BigDecimal(args[args.length - 1]);
        }
        if (args.length >= 3) {
          accountName = args[args.length - 2];
        }
        addressList.addAll(Arrays.asList(args).subList(1, args.length - 2));
        Recipient recipient = new Recipient();
        recipient.setAmount(amount);
        recipient.setRecipientAddress(accountName);
        System.out
            .println(wallet.createTransaction(addressList, Collections.singletonList(recipient)));
        break;
      case "signTransaction":
        if (args.length == 4) {
          accountName = args[3];
        }
        if (args.length >= 3) {
          address = args[2];
        }
        if (args.length >= 2) {
          transaction = args[1];
        }
        if (args.length < 4) {
          System.out.println(wallet.signTransaction(transaction, address));
        } else {
          System.out.println(wallet.signTransaction(transaction, address, accountName));
        }
        break;
      case "getSignersForTransaction":
        if (args.length >= 2) {
          transaction = args[1];
        }
        System.out.println(wallet.getSignersForTransaction(transaction));
        break;
      case "sendTransaction":
        if (args.length >= 2) {
          transaction = args[1];
        }
        System.out.println(wallet.sendTransaction(transaction));
        break;
      case "monitor":
        if (args.length >= 2) {
          accountName = args[1];
        }
        System.out.println("*This is a testing function*");
        System.out.println("*It will pause for several minutes to evaluate the network,"
            + " a full run can take 8+ minutes*");
        System.out.println("Adding " + accountName + " to monitor...");
        monitor.addAddresses(Collections.singletonList(accountName));
        System.out.println("Initial values...");
        monitor.getBalances().forEach(
            (balanceAddress, balance) -> System.out.println(balanceAddress + ": " + balance));
        System.out.println("2 minute sleep to load...");
        Thread.sleep(1000 * 120);
        monitor.getBalances().forEach(
            (balanceAddress, balance) -> System.out.println(balanceAddress + ": " + balance));
        monitor.getObservableBalances().subscribe(balanceMap -> {
          System.out.println("Observable updated:");
          balanceMap.forEach(
              (balanceAddress, balance) -> System.out.println(balanceAddress + ": " + balance));
        });
        System.out.println("60 second sleep...");
        Thread.sleep(60 * 1000);
        System.out.println("60 second sleep...");
        Thread.sleep(60 * 1000);
        System.out.println("60 second sleep...");
        Thread.sleep(60 * 1000);
        System.out.println("60 second sleep...");
        Thread.sleep(60 * 1000);
        System.out.println("60 second sleep...");
        Thread.sleep(60 * 1000);
        System.out.println("60 second sleep...");
        Thread.sleep(60 * 1000);
        break;
      case "listTxs":
        if (args.length >= 2) {
          accountName = args[1];
        }
        if (args.length >= 3) {
          resultSize = Integer.parseInt(args[2]);
        }
        if (args.length >= 4) {
          skipNumber = Integer.parseInt(args[3]);
        }
        Arrays.asList(wallet.getTransactions(accountName, resultSize, skipNumber))
            .forEach(System.out::println);
        break;
      case "getTx":
        if (args.length >= 2) {
          accountName = args[1];
        }
        System.out.println(wallet.getTransaction(accountName));
        break;
      default:
        System.out.println("Method not valid or not supported yet");
    }
  }
}
