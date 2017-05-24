package io.emax.cosigner.core.cluster.commands;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.emax.cosigner.api.core.Server;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.crypto.Aes;
import io.emax.cosigner.common.crypto.Secp256k1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EncryptedCommand implements BaseCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedCommand.class);
  // Track nonces for other servers in both directions.
  private static final Map<String, Long> incomingNonces = new HashMap<>();
  private static final Map<String, Long> outgoingNonces = new HashMap<>();

  private Server sender;
  private String payload;
  private String iv;
  private long nonce;

  public EncryptedCommand() {

  }

  /**
   * Encrypts a command to be sent to the recipient.
   */
  public EncryptedCommand(Server sender, byte[] senderKey, Server recipient, String data) {
    try {
      this.iv = ByteUtilities.toHexString(Aes.generateIv());

      // Nonce lists will be reset on app restart, but the serverID should be a new public key. This
      // means the nonce lists should all be trying to talk to a new entry, and should all agree on
      // the nonce.

      // Set up the nonce
      DecryptedPayload decryptedPayload = new DecryptedPayload();
      if (!outgoingNonces.containsKey(recipient.getServerId())) {
        outgoingNonces.put(recipient.getServerId(), 1L);
      }
      decryptedPayload.setNonce(outgoingNonces.get(recipient.getServerId()));
      this.nonce = decryptedPayload.getNonce();
      outgoingNonces.put(recipient.getServerId(), decryptedPayload.getNonce() + 1L);
      decryptedPayload.setPayload(data);
      data = ByteUtilities.toHexString(decryptedPayload.toJson().getBytes("UTF-8"));
      this.sender = sender;

      byte[] otherKey = ByteUtilities.toByteArray(recipient.getServerId());
      byte[] sharedKey = Secp256k1.generateSharedSecret(senderKey, otherKey);
      this.payload = Aes.encrypt(sharedKey, ByteUtilities.toByteArray(iv), data);
    } catch (Exception e) {
      LOGGER.error(null, e);
    }
  }

  public Server getSender() {
    return sender;
  }

  public String getPayload() {
    return payload;
  }

  public String getIv() {
    return iv;
  }

  public long getNonce() {
    return nonce;
  }

  public void setSender(Server sender) {
    this.sender = sender;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public void setIv(String iv) {
    this.iv = iv;
  }

  public void setNonce(long nonce) {
    this.nonce = nonce;
  }

  @Override
  public String toJson() {
    try {
      JsonFactory jsonFact = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(jsonFact);
      ObjectWriter writer = mapper.writerFor(EncryptedCommand.class);
      return writer.writeValueAsString(this);
    } catch (IOException e) {
      LOGGER.warn(null, e);
      return "";
    }
  }

  @Override
  public String toString() {
    return "EncryptedCommand [sender=" + sender + ", payload=" + payload + ", iv=" + iv + ", nonce="
        + nonce + "]";
  }

  /**
   * Parse a JSON string that represents a ClusterCommand..
   */
  public static EncryptedCommand parseCommandString(String commandString) {
    try {
      JsonFactory jsonFact = new JsonFactory();
      JsonParser jsonParser = jsonFact.createParser(commandString);
      return new ObjectMapper().readValue(jsonParser, EncryptedCommand.class);
    } catch (IOException e) {
      LOGGER.debug("Problem parsing Encrypted Command, may be another type.");
      LOGGER.trace(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Attempt to handle the request in a cluster command.
   */
  public static String handleCommand(byte[] recipientKey, EncryptedCommand command) {
    try {
      byte[] senderKey = ByteUtilities.toByteArray(command.getSender().getServerId());
      byte[] sharedKey = Secp256k1.generateSharedSecret(recipientKey, senderKey);

      // Decrypt the payload
      String data =
          Aes.decrypt(sharedKey, ByteUtilities.toByteArray(command.getIv()), command.getPayload());
      data = new String(ByteUtilities.toByteArray(data), "UTF-8");
      DecryptedPayload payload = DecryptedPayload.parseData(data);

      // Validate the nonce.
      if (!incomingNonces.containsKey(command.sender.getServerId())) {
        incomingNonces.put(command.sender.getServerId(), 0L);
      }
      long expectedNonce = incomingNonces.get(command.sender.getServerId());
      if (payload.getNonce() <= expectedNonce) {
        // Nonce is too low.
        throw new Exception("NONCE is too low from server: " + command.sender.getServerId());
      }
      // Update so we only accept newer nonces after this command.
      incomingNonces.put(command.sender.getServerId(), payload.getNonce());

      return payload.getPayload();
    } catch (Exception e) {
      LOGGER.error(null, e);
      return "";
    }
  }

}
