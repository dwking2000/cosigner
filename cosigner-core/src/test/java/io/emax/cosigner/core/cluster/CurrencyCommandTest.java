package io.emax.heimdal.core.cluster;

import java.util.Arrays;
import java.util.LinkedList;

import org.junit.Test;

import io.emax.heimdal.core.Application;
import io.emax.heimdal.core.ApplicationConfiguration;
import io.emax.heimdal.core.currency.Common;
import io.emax.heimdal.core.currency.CommonTest;
import io.emax.heimdal.core.currency.CurrencyParameters;
import junit.framework.TestCase;

public class CurrencyCommandTest extends TestCase {
  private static String signingAccount = "beef";
  private static String sigString = "deadbeef";

  @Override
  public void setUp() {
    Application.setConfig(new ApplicationConfiguration());
    // Register currencies in case this is run before CommonTest
    CommonTest currencyTest = new CommonTest();
    currencyTest.setUp();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testHandleCommand() {
    System.out.println("");
    System.out.println("Testing network-requested tx signing");

    LinkedList<String> currencies = new LinkedList<>();
    try {
      String currenciesString = Common.listCurrencies();
      currencies = (LinkedList<String>) Common.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Problem listing currencies.");
    }

    currencies.forEach(currency -> {
      CurrencyParameters params = new CurrencyParameters();
      params.setCurrencySymbol(currency);
      params.setTransactionData(sigString);
      params.setAccount(Arrays.asList(signingAccount));

      System.out.println("Asking for a " + currency + " signature for key: " + signingAccount
          + " on data: " + sigString);

      CurrencyCommand command = new CurrencyCommand();
      command.setCommandType(CurrencyCommandType.SIGN);
      command.setCurrencyParams(params);

      String response = CurrencyCommand.handleCommand(command);
      command = CurrencyCommand.parseCommandString(response);

      System.out.println("Response to sign-request: " + command.toJson());

    });
  }
}
