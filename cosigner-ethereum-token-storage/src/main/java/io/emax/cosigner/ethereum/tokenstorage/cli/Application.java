package io.emax.cosigner.ethereum.tokenstorage.cli;

import io.emax.cosigner.api.currency.Wallet.Recipient;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.ethereum.core.EthereumResource;
import io.emax.cosigner.ethereum.core.gethrpc.EthereumRpc;
import io.emax.cosigner.ethereum.core.gethrpc.RawTransaction;
import io.emax.cosigner.ethereum.tokenstorage.Configuration;
import io.emax.cosigner.ethereum.tokenstorage.Monitor;
import io.emax.cosigner.ethereum.tokenstorage.Setup;
import io.emax.cosigner.ethereum.tokenstorage.Utilities;
import io.emax.cosigner.ethereum.tokenstorage.Wallet;
import io.emax.cosigner.ethereum.tokenstorage.contract.Contract;
import io.emax.cosigner.ethereum.tokenstorage.contract.v1.ContractParametersV1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Command line option for running the library.
 *
 * @author Tom
 */
public class Application {
  /**
   * Command line interface that provides basic access to wallet.
   *
   * @param args Commands, run with none to see usage.
   * @throws InterruptedException Monitor and transaction usage requires time to load data, sleep
   *                              throws this exception.
   */
  public static void main(String[] args) throws InterruptedException, IOException {
    if (args.length < 1) {
      System.out.println("Usage: <interfaceMethod> <argument> <argument> ...");
      System.out.println("Available methods:");
      System.out.println("\tgetNewAddress(String accountName)");
      System.out.println("\tgetDeterministicAddresses(String accountName)");
      System.out
          .println("\tgetMultiSigAddress(String address1, String address2, ..., accountName)");
      System.out.println("\tgetBalance(String address)");
      System.out.println("\tgetPendingBalance(String address)");
      System.out.println("\tgetTotalBalances()");
      System.out.println("\tcreateTransaction(String fromAddress1, String fromAddress2,"
          + " ..., String toAddress, Decimal amount)");
      System.out.println("\tsignTransaction(String transaction, String address)");
      System.out.println("\tsignTransactionWithKey(String transaction, String address)");
      System.out.println("\tsendTransaction(String transaction)");
      System.out.println("\tmonitor(String address)");
      System.out.println("\tlistTxs(String address, int resultSize, int skipNumber)");
      System.out.println("\tgetTx(String txid)");
      System.out.println("\tgeneratePrivateKey()");
      System.out.println("\tgenerateAddressFromKey(privateKey)");
      System.out.println("\tgenerateTokens(String recipient, Long tokens)");
      System.out.println("\tdestroyTokens(String sender, Long tokens)");
      System.out.println("\tdeposit(String recipient, Long tokens)");
      System.out.println("\treconcile(String affectedAddress, Long tokenChange)");
      System.out.println("\tsetTokenContract()");
      System.out.println("\tGenerateContracts(String currency, bool apply)");
      return;
    }

    Configuration config = new Configuration("EUR");
    Wallet wallet = new Wallet(config);
    Monitor monitor = new Monitor(wallet);
    String accountName = "";
    String address = "";
    String transaction = "";
    LinkedList<String> addressList = new LinkedList<>();
    BigDecimal amount = BigDecimal.ZERO;
    BigInteger bigIntVal = BigInteger.ZERO;
    Long longVal = 0L;
    Boolean boolVal = false;
    RawTransaction tx = null;
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
      case "getPendingBalance":
        if (args.length >= 2) {
          accountName = args[1];
        }
        System.out.println(wallet.getPendingBalance(accountName));
        break;
      case "getTotalBalances":
        System.out.println(wallet.getTotalBalances());
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
      case "signTransactionWithKey":
        if (args.length >= 3) {
          address = args[2];
        }
        if (args.length >= 2) {
          transaction = args[1];
        }
        System.out.print("Private Key: ");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String key = reader.readLine();
        System.out.println(wallet.signTransaction(transaction, address, key));
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
        Thread.sleep(1000 * 120L);
        monitor.getBalances().forEach(
            (balanceAddress, balance) -> System.out.println(balanceAddress + ": " + balance));
        monitor.getObservableBalances().subscribe(balanceMap -> {
          System.out.println("Observable updated:");
          balanceMap.forEach(
              (balanceAddress, balance) -> System.out.println(balanceAddress + ": " + balance));
        });
        System.out.println("60 second sleep...");
        Thread.sleep(60 * 1000L);
        System.out.println("60 second sleep...");
        Thread.sleep(60 * 1000L);
        System.out.println("60 second sleep...");
        Thread.sleep(60 * 1000L);
        System.out.println("60 second sleep...");
        Thread.sleep(60 * 1000L);
        System.out.println("60 second sleep...");
        Thread.sleep(60 * 1000L);
        System.out.println("60 second sleep...");
        Thread.sleep(60 * 1000L);
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
      case "generatePrivateKey":
        System.out.println(wallet.generatePrivateKey());
        break;
      case "generateAddressFromKey":
        if (args.length >= 2) {
          accountName = args[1];
        }
        System.out.println(wallet.createAddressFromKey(accountName, true));
        break;
      case "generateTokens":
        if (args.length >= 2) {
          accountName = args[1];
        }
        if (args.length >= 3) {
          longVal = new BigInteger(args[2]).longValue();
        }
        System.out.println(Utilities.generateTokens(accountName, longVal, config));
        break;
      case "destroyTokens":
        if (args.length >= 2) {
          accountName = args[1];
        }
        if (args.length >= 3) {
          longVal = new BigInteger(args[2]).longValue();
        }
        System.out.println(Utilities.destroyTokens(accountName, longVal, config));
        break;
      case "deposit":
        if (args.length >= 2) {
          accountName = args[1];
        }
        if (args.length >= 3) {
          longVal = new BigInteger(args[2]).longValue();
        }
        tx = RawTransaction.createTransaction(config, config.getTokenContractAddress(), null,
            new Contract().getContractParameters()
                .deposit(config, accountName, BigInteger.valueOf(longVal)));
        System.out.println(ByteUtilities.toHexString(tx.encode()));
        break;
      case "reconcile":
        if (args.length >= 2) {
          accountName = args[1];
        }
        if (args.length >= 3) {
          bigIntVal = new BigInteger(args[2]);
        }
        HashMap<String, BigInteger> balanceChanges = new HashMap<>();
        balanceChanges.put(accountName, bigIntVal);
        System.out.println(Utilities.reconcile(balanceChanges, config));
        break;
      case "setTokenContract":
        ContractParametersV1 contractInterface = new ContractParametersV1();
        EthereumRpc ethereumRpc = EthereumResource.getResource().getEthWriteRPC();
        Long nonce = contractInterface
            .getNonce(ethereumRpc, config.getAdminContractAddress(), config.getAdminAccount());
        tx = RawTransaction.createTransaction(config, config.getAdminContractAddress(), null,
            contractInterface
                .setTokenChild(nonce, config.getTokenContractAddress(), new LinkedList<>(),
                    new LinkedList<>(), new LinkedList<>()));
        System.out.println(ByteUtilities.toHexString(tx.encode()));
        break;
      case "GenerateContracts":
        if (args.length >= 2) {
          accountName = args[1];
        }
        boolVal = false;
        if (args.length >= 3) {
          boolVal = Boolean.valueOf(args[2]);
        }
        config = new Configuration(accountName);
        config.generateNewContract(boolVal);
        Setup.setupTokenContract(config);
        break;
      default:
        System.out.println("Method not valid or not supported yet");
    }
  }
}
