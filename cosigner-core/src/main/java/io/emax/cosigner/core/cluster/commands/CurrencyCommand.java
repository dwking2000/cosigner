package io.emax.cosigner.core.cluster.commands;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.emax.cosigner.api.core.CurrencyParameters;
import io.emax.cosigner.common.Json;
import io.emax.cosigner.core.currency.Common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CurrencyCommand implements BaseCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(CurrencyCommand.class);
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

  /**
   * Convert this command to a JSON string.
   */
  @Override
  public String toJson() {
    try {
      JsonFactory jsonFact = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(jsonFact);
      ObjectWriter writer = mapper.writerFor(CurrencyCommand.class);
      return writer.writeValueAsString(this);
    } catch (IOException e) {
      LOGGER.warn(null, e);
      return "";
    }
  }

  @Override
  public String toString() {
    return "CurrencyCommand [commandType=" + commandType + ", currencyParams=" + currencyParams
        + "]";
  }

  /**
   * Parse a JSON string that represents a CurrencyCommand.
   * 
   * @param commandString JSON string to parse.
   * @return CurrencyCommand that the string represents.
   */
  public static CurrencyCommand parseCommandString(String commandString) {
    try {
      JsonFactory jsonFact = new JsonFactory();
      JsonParser jsonParser = jsonFact.createParser(commandString);
      return new ObjectMapper().readValue(jsonParser, CurrencyCommand.class);
    } catch (IOException e) {
      LOGGER.warn(null, e);
      return null;
    }
  }

  /**
   * Attempt to handle the request in a currency command.
   * 
   * @param command The command received.
   * @return The string response to return to the requestor.
   */
  public static String handleCommand(CurrencyCommand command) {
    // SIGN -- Attempt to sign the provided data, for off-client keys
    switch (command.commandType) {
      case SIGN:
        String signedTx = Common.approveTransaction(
            Json.stringifyObject(CurrencyParameters.class, command.currencyParams), false);
        command.currencyParams.setTransactionData(signedTx);
        return command.toJson();
      default:
        return command.toJson();
    }
  }
}
