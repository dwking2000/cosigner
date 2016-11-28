package io.emax.cosigner.admin.cli;

import io.emax.cosigner.admin.AdminConnector;
import io.emax.cosigner.api.core.Server;
import io.emax.cosigner.common.Json;

import java.util.HashMap;

public class AdminApplication {
  /**
   * ListNodes AddNode SetClusterKey GetClusterKey SetLocation.
   */

  /**
   * Command line interface that provides basic access to the library.
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: <interfaceMethod> <argument> <argument> ...");
      System.out.println("Available methods:");
      System.out.println("\tListNodes()");
      System.out.println("\tAddNode(Server)");
      System.out.println("\t\tServer: See results from ListNodes");
      System.out.println("\tGetClusterKey()");
      System.out.println("\tSetClusterKey(key)");
      System.out.println("\tSetLocation(URIString)");
      System.out.println("\tLoadEthToken(TokenSymbol)");
      System.out.println("\tGetConfigurations()");
      System.out.println("\tGetChainHeight(Currency)");
      System.out.println("\tGetLastBlockTime(Currency)");
      return;
    }

    AdminConnector adminConnection = new AdminConnector();
    Server server;
    String stringInput = "";
    switch (args[0]) {
      case "ListNodes":
        System.out.println(adminConnection.listNodes());
        break;
      case "AddNode":
        if (args.length >= 2) {
          stringInput = args[1];
        }
        server = (Server) Json.objectifyString(Server.class, stringInput);
        System.out.println(adminConnection.addNode(server));
        break;
      case "GetClusterKey":
        System.out.println(adminConnection.getClusterKey());
        break;
      case "SetClusterKey":
        if (args.length >= 2) {
          stringInput = args[1];
        }
        adminConnection.setClusterKey(stringInput);
        System.out.println("Set the cluster key");
        break;
      case "SetLocation":
        if (args.length >= 2) {
          stringInput = args[1];
        }
        adminConnection.setServerLocation(stringInput);
        System.out.println("Set the server location");
        break;
      case "GetConfigurations":
        System.out.println(Json.stringifyObject(HashMap.class, adminConnection.getConfigurations()));
        break;
      case "LoadEthToken":
        if (args.length >= 2) {
          stringInput = args[1];
        }
        adminConnection.loadEthToken(stringInput);
        System.out.println("Loaded eth token [" + stringInput + "]");
        break;
      case "GetChainHeight":
        if (args.length >= 2) {
          stringInput = args[1];
        }
        System.out.println(adminConnection.getBlockchainHeight(stringInput));
        break;
      case "GetLastBlockTime":
        if (args.length >= 2) {
          stringInput = args[1];
        }
        System.out.println(adminConnection.getLastBlockTime(stringInput));
        break;
      default:
        System.out.println("Method not valid or not supported yet");
    }
  }
}
