package io.emax.heimdal.ethereum.cli;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedList;

import io.emax.heimdal.ethereum.Monitor;
import io.emax.heimdal.ethereum.Wallet;

/**
 * Command line option for running the library
 * 
 * @author Tom
 *
 */
public class Application {
	public static void main(String[] args) throws InterruptedException {
		if (args.length < 1) {
			System.out.println("Usage: <interfaceMethod> <argument> <argument> ...");
			System.out.println("Available methods:");
			System.out.println("	getNewAddress(String accountName)");
			System.out.println("	getDeterministicAddresses(String accountName)");
			System.out.println("	getMultiSigAdress(String address1, String address2, ..., accountName)");
			System.out.println("	getBalance(String address)");
			System.out.println("	createTransaction(String fromAddress1, String fromAddress2, ..., String toAddress, Decimal amount)");
			System.out.println("	signTransaction(String transaction, String address)");
			System.out.println("	sendTransaction(String transaction)");
			System.out.println("	monitor(String address)");
			return;
		}

		Wallet wallet = new Wallet();
		Monitor monitor = new Monitor();
		String accountName = "";
		String address = "";
		String transaction = "";
		LinkedList<String> addressList = new LinkedList<>();
		BigDecimal amount = BigDecimal.ZERO;
		switch (args[0]) {
		case "getNewAddress":
			if (args.length >= 2)
				accountName = args[1];
			System.out.println(wallet.createAddress(accountName));
			break;
		case "getDeterministicAddresses":
			if (args.length >= 2)
				accountName = args[1];
			wallet.getAddresses(accountName).forEach(System.out::println);
			break;
		case "getMultiSigAddress":
			if (args.length >= 2) {
				accountName = args[args.length - 1];
			}
			for (int i = 1; i < args.length - 1; i++) {
				addressList.add(args[i]);
			}
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
			for (int i = 1; i < args.length - 2; i++) {
				addressList.add(args[i]);
			}
			System.out.println(wallet.createTransaction(addressList, accountName, amount));
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
			if(args.length < 4)
				System.out.println(wallet.signTransaction(transaction, address));
			else
				System.out.println(wallet.signTransaction(transaction, address, accountName));
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
			System.out.println("*It will pause for several minutes to evaluate the network, a full run can take 8+ minutes*");
			System.out.println("Adding " + accountName + " to monitor...");
			monitor.addAddresses(Arrays.asList(accountName));
			System.out.println("Initial values...");
			monitor.getBalances().forEach((balanceAddress, balance) -> {
				System.out.println(balanceAddress + ": " + balance);
			});
			System.out.println("2 minute sleep to load...");
			Thread.sleep(1000*120);
			monitor.getBalances().forEach((balanceAddress, balance) -> {
				System.out.println(balanceAddress + ": " + balance);
			});
			monitor.getObservableBalances().subscribe(balanceMap -> {
				System.out.println("Observable updated:");
				balanceMap.forEach((balanceAddress, balance) -> {
					System.out.println(balanceAddress + ": " + balance);
				});
			});		
			System.out.println("60 second sleep...");
			Thread.sleep(60*1000);
			System.out.println("60 second sleep...");
			Thread.sleep(60*1000);
			System.out.println("60 second sleep...");
			Thread.sleep(60*1000);
			System.out.println("60 second sleep...");
			Thread.sleep(60*1000);
			System.out.println("60 second sleep...");
			Thread.sleep(60*1000);
			System.out.println("60 second sleep...");
			Thread.sleep(60*1000);
			break;
		default:
			System.out.println("Method not valid or not supported yet");
		}
	}
}
