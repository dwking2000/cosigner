package io.emax.heimdal.blockchains.ethereum;

import io.emax.heimdal.blockchains.ethereum.Client.Transactor;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.jetbrains.annotations.NotNull;

public class MultiSigWalletFactory {

  private Transactor transactor1, transactor2;
  private final String coldStorage;

  public MultiSigWalletFactory(@NotNull Transactor transactor1, @NotNull Transactor monitor2,
      String coldStorage) {
    this.transactor1 = transactor1;
    this.transactor2 = monitor2;
    this.coldStorage = coldStorage;
  }

  public String getColdStorage() {
    return coldStorage;
  }

  /**
   * Create a multisig wallet contract
   *
   * @return A future containing the multisig wallet contract
   */
  public MultiSigWallet makeNewMultiSigWallet() throws InterruptedException, ExecutionException,
      IOException {
    // TODO: This needs to be deterministic and saved to a database
    String address = "0x" + new BigInteger(256, new Random()).toString(16);
    return new MultiSigWallet(address, transactor1.coinbase(), transactor2.coinbase(),
        getColdStorage());
  }
}
