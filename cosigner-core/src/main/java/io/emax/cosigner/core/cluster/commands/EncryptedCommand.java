package io.emax.cosigner.core.cluster.commands;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.crypto.Aes;
import io.emax.cosigner.common.crypto.Secp256k1;
import io.emax.cosigner.core.cluster.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class EncryptedCommand implements BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(EncryptedCommand.class);

  private Server sender;
  private String payload;
  private String iv;

  public Server getSender() {
    return sender;
  }

  public String getPayload() {
    return payload;
  }

  public String getIv() {
    return iv;
  }

  /**
   * Encrypts a command to be sent to the recipient.
   */
  public EncryptedCommand(Server sender, byte[] senderKey, Server recipient, String data) {
    try {
      byte[] myKey = senderKey;
      byte[] otherKey = ByteUtilities.toByteArray(recipient.getServerId());

      this.iv = ByteUtilities.toHexString(Aes.generateIv());

      byte[] sharedKey = Secp256k1.generateSharedSecret(myKey, otherKey);
      data = ByteUtilities.toHexString(data.getBytes("UTF-8"));
      this.sender = sender;
      this.payload = Aes.encrypt(sharedKey, ByteUtilities.toByteArray(iv), data);
    } catch (Exception e) {
      logger.error(null, e);
    }
  }

  @Override
  public String toJson() {
    try {
      JsonFactory jsonFact = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(jsonFact);
      ObjectWriter writer = mapper.writerFor(EncryptedCommand.class);
      return writer.writeValueAsString(this);
    } catch (IOException e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      logger.warn(errors.toString());
      return "";
    }
  }

  @Override
  public String toString() {
    return "EncryptedCommand [sender=" + sender + ", payload=" + payload + ", iv=" + iv + "]";
  }

  /**
   * Parse a JSON string that represents a ClusterCommand..
   */
  public static EncryptedCommand parseCommandString(String commandString) {
    try {
      JsonFactory jsonFact = new JsonFactory();
      JsonParser jsonParser = jsonFact.createParser(commandString);
      EncryptedCommand encryptedCommand =
          new ObjectMapper().readValue(jsonParser, EncryptedCommand.class);
      return encryptedCommand;
    } catch (IOException e) {
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      logger.warn(errors.toString());
      return null;
    }
  }

  /**
   * Attempt to handle the request in a cluster command.
   */
  public static String handleCommand(byte[] recipientKey, EncryptedCommand command) {
    try {
      byte[] senderKey = ByteUtilities.toByteArray(command.getSender().getServerId());
      byte[] myKey = recipientKey;
      byte[] sharedKey = Secp256k1.generateSharedSecret(myKey, senderKey);

      String data =
          Aes.decrypt(sharedKey, ByteUtilities.toByteArray(command.getIv()), command.getPayload());
      data = new String(ByteUtilities.toByteArray(data), "UTF-8");
      return data;
    } catch (Exception e) {
      logger.error(null, e);
      return "";
    }
  }

}
