package io.emax.heimdal.core.cluster;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.emax.heimdal.core.currency.Common;
import io.emax.heimdal.core.currency.CurrencyParameters;

public class CurrencyCommand implements BaseCommand {
  private CurrencyCommandType commandType;
  private CurrencyParameters currencyParams;

  public CurrencyCommandType getCommandType() {
    return commandType;
  }

  public void setCommandType(CurrencyCommandType commandType) {
    this.commandType = commandType;
  }

  public CurrencyParameters getCurrencyParams() {
    return currencyParams;
  }

  public void setCurrencyParams(CurrencyParameters currencyParams) {
    this.currencyParams = currencyParams;
  }

  public String toJson() {
    try {
      JsonFactory jsonFact = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(jsonFact);
      ObjectWriter writer = mapper.writerFor(CurrencyCommand.class);
      return writer.writeValueAsString(this);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return "";
    }
  }

  public static CurrencyCommand parseCommandString(String commandString) {
    try {
      JsonFactory jsonFact = new JsonFactory();
      JsonParser jsonParser = jsonFact.createParser(commandString);
      CurrencyCommand currencyCommand =
          new ObjectMapper().readValue(jsonParser, CurrencyCommand.class);
      return currencyCommand;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  public static String handleCommand(CurrencyCommand command) {
    // SIGN -- Attempt to sign the provided data, for off-client keys
    switch (command.commandType) {
      case SIGN:
        String signedTx = Common.approveTransaction(Common.stringifyObject(CurrencyParameters.class, command.currencyParams), false);
        command.currencyParams.setTransactionData(signedTx);
        return command.toJson();
      default:
        return command.toJson();
    }
  }
}
