package io.emax.cosigner.core.cluster;

import java.util.Arrays;
import java.util.LinkedList;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.emax.cosigner.api.core.CurrencyParameters;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.core.CosignerApplication;
import io.emax.cosigner.core.CosignerConfiguration;
import io.emax.cosigner.core.cluster.commands.CurrencyCommand;
import io.emax.cosigner.core.cluster.commands.CurrencyCommandType;
import io.emax.cosigner.core.currency.Common;
import io.emax.cosigner.core.currency.CommonTest;

import junit.framework.TestCase;

public class CurrencyCommandTest extends TestCase {
  private static final Logger LOGGER = LoggerFactory.getLogger(CurrencyCommandTest.class);
  private static String signingAccount = "beef";
  private static String sigString = "deadbeef";

  @Override
  public void setUp() {
    CosignerApplication.setConfig(new CosignerConfiguration());
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
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
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
