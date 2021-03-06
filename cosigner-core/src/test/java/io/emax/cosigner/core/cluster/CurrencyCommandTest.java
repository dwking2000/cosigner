package io.emax.cosigner.core.cluster;

import io.emax.cosigner.api.core.CosignerResponse;
import io.emax.cosigner.api.core.CurrencyParameters;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.core.CosignerApplication;
import io.emax.cosigner.core.CosignerConfiguration;
import io.emax.cosigner.core.cluster.commands.CurrencyCommand;
import io.emax.cosigner.core.cluster.commands.CurrencyCommandType;
import io.emax.cosigner.core.currency.Common;
import io.emax.cosigner.core.currency.CommonTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;

public class CurrencyCommandTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(CurrencyCommandTest.class);
  private static final String signingAccount = "beef";
  private static final String sigString = "deadbeef";

  @Before
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
      CosignerResponse cosignerResponse =
          (CosignerResponse) Json.objectifyString(CosignerResponse.class, currenciesString);
      currenciesString = cosignerResponse.getResult();
      currencies = (LinkedList<String>) Json.objectifyString(LinkedList.class, currenciesString);
    } catch (Exception e) {
      LOGGER.debug(null, e);
      Assert.fail("Problem listing currencies.");
    }

    currencies.forEach(currency -> {
      CurrencyParameters params = new CurrencyParameters();
      params.setCurrencySymbol(currency);
      params.setTransactionData(sigString);
      params.setAccount(Collections.singletonList(signingAccount));

      System.out.println(
          "Asking for a " + currency + " signature for key: " + signingAccount + " on data: "
              + sigString);

      CurrencyCommand command = new CurrencyCommand();
      command.setCommandType(CurrencyCommandType.SIGN);
      command.setCurrencyParams(params);

      String response = CurrencyCommand.handleCommand(command);
      CosignerResponse cosignerResponse =
          (CosignerResponse) Json.objectifyString(CosignerResponse.class, response);
      response = cosignerResponse.getResult();

      System.out.println("Response to sign-request: " + response);

    });
  }
}
