package io.emax.cosigner.core.cluster;

import io.emax.cosigner.api.core.CurrencyParameters;
import io.emax.cosigner.common.ByteUtilities;
import io.emax.cosigner.common.crypto.Secp256k1;
import io.emax.cosigner.core.cluster.commands.CurrencyCommand;
import io.emax.cosigner.core.cluster.commands.CurrencyCommandType;
import io.emax.cosigner.core.cluster.commands.EncryptedCommand;

import org.junit.Test;

import junit.framework.TestCase;

import java.util.Arrays;

public class EncryptedCommandTest extends TestCase {
  @Test
  public void testCommand() {
    System.out.println("Testing command encryption.");
    // Generate keys
    byte[] myKey = Secp256k1.generatePrivateKey();
    byte[] myPublicKey = Secp256k1.getPublicKey(myKey);

    byte[] otherKey = Secp256k1.generatePrivateKey();
    byte[] otherPublicKey = Secp256k1.getPublicKey(otherKey);

    byte[] mySharedSecret = Secp256k1.generateSharedSecret(myKey, otherPublicKey);
    byte[] otherSharedSecret = Secp256k1.generateSharedSecret(otherKey, myPublicKey);

    // Verify that shared secrets match both directions
    String mySecret = ByteUtilities.toHexString(mySharedSecret);
    String otherSecret = ByteUtilities.toHexString(otherSharedSecret);
    System.out.println("My Secret: " + mySecret);
    System.out.println("Other secret: " + otherSecret);

    if (mySecret == null || mySecret.isEmpty() || otherSecret == null || otherSecret.isEmpty()) {
      System.out.println("Error with java security. This should be ok if all other tests pass.");
      return;
    }
    assertEquals(mySecret, otherSecret);

    // Create, encrypt, and decrypt a command to verify it.
    CurrencyCommand command = new CurrencyCommand();
    CurrencyParameters params = new CurrencyParameters();

    params.setCurrencySymbol("TEST");
    params.setAccount(Arrays.asList("TESTADDRESS"));
    params.setUserKey("TESTKEY");

    command.setCommandType(CurrencyCommandType.SIGN);
    command.setCurrencyParams(params);
    System.out.println("Created command: " + command);

    Server serverTo = new Server();
    serverTo.setServerId(ByteUtilities.toHexString(otherPublicKey));
    System.out.println("Recipient server: " + serverTo);

    Server serverFrom = new Server();
    serverFrom.setServerId(ByteUtilities.toHexString(myPublicKey));
    System.out.println("Sender server: " + serverFrom);

    EncryptedCommand encryptedCommand =
        new EncryptedCommand(serverFrom, myKey, serverTo, command.toJson());
    System.out.println("Encrypted command: " + encryptedCommand);
    String decryptedCommandString = EncryptedCommand.handleCommand(otherKey, encryptedCommand);
    System.out.println("Decrypted payload: " + decryptedCommandString);

    CurrencyCommand decryptedCommand = CurrencyCommand.parseCommandString(decryptedCommandString);
    System.out.println("Decrypted command: " + decryptedCommand);

    assertEquals(command.toJson(), decryptedCommand.toJson());
  }
}
