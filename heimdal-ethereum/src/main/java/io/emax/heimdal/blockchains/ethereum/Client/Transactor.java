package io.emax.heimdal.blockchains.ethereum.Client;

import static java.util.Collections.singletonList;
import io.emax.heimdal.blockchains.JSONRPC2;
import io.emax.heimdal.blockchains.ethereum.Contract;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;

public class Transactor {

  private final URI uri;
  private AtomicReference<String> coinbase = new AtomicReference<String>("");

  /**
   * Creates a new RestfulTransactor
   *
   * @param url URL of the Ethereum signer RPC service
   * @throws URISyntaxException
   */
  public Transactor(@NotNull String url) throws URISyntaxException {
    uri = new URI(url);
  }

  /**
   * Get the RestfulTransactor's coinbase.
   *
   * See https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_coinbase
   *
   * @return The hex code for a coinbase
   */
  public String coinbase() throws IOException {
    if (Objects.equals(coinbase.get(), ""))
      coinbase.set(new Gson().fromJson(JSONRPC2.Call(uri, "eth_coinbase"), Properties.class)
          .getProperty("result"));
    return coinbase.get();
  }

  /**
   * Create a contract
   *
   * See https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_sendtransaction
   *
   * @param contract The contract to be created on the Ethereum blockchain
   * @param gasPrice The gasPrice to pay
   * @return The address of the created contract
   * @throws IOException
   */
  public String createContract(Contract contract, BigInteger gasPrice) throws IOException {
    List<Map<String, String>> params = singletonList(new HashMap<String, String>() {
      {
        put("from", coinbase());
        put("to", contract.getAddress());
        put("gas", "0x" + Integer.valueOf(String.valueOf(1000), 16));
        put("gasPriceAsHex", "0x" + gasPrice.toString(16));
        put("data", contract.getCode());
      }
    });
    String jsonRPCOutput = JSONRPC2.Call(uri, "eth_sendTransaction", params);
    Properties jsonRPCReturn = new Gson().fromJson(jsonRPCOutput, Properties.class);
    return jsonRPCReturn.getProperty("result");
  }

  /**
   * Create a contract
   *
   * See https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_sendtransaction
   *
   * @param contract The contract to be created on the Ethereum blockchain
   * @param gasPriceAsHexString The gasPrice to pay as a hexadecimal string
   * @return The address of the created contract
   * @throws IOException
   */
  public String createContract(Contract contract, String gasPriceAsHexString) throws IOException {
    return createContract(contract, new BigInteger(gasPriceAsHexString.replace("^0x", ""), 16));
  }

  /**
   * Get the current gas price
   *
   * See https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_gasprice
   *
   * @return A string representing the current gas price
   */
  public String gasPriceAsHex() throws IOException {
    return new Gson().fromJson(JSONRPC2.Call(uri, "eth_gasPrice"), Properties.class).getProperty(
        "result");
  }

  /**
   * Create a contract
   *
   * See https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_sendtransaction
   *
   * @param contract The contract to be created on the Ethereum blockchain
   * @return The address of the created contract
   * @throws ExecutionException
   * @throws InterruptedException
   * @throws IOException
   */
  public String createContract(Contract contract) throws ExecutionException, InterruptedException,
      IOException {
    return createContract(contract, gasPriceAsHex());
  }
}
