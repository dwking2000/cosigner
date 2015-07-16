package io.emax.heimdal.blockchains.ethereum;

import io.emax.heimdal.internal.Pool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Future;

import org.jetbrains.annotations.NotNull;

public class Solidity {

  /**
   * Compiles contract code using the solidity compiler. Returns a {Future<String>} containing the
   * compiled code.
   *
   * @param contractCode Contract Code To be Compiled
   * @return Compiled code in a {Future<String>}
   * @throws IOException
   * @throws InterruptedException
   */
  public static Future<String> compile(@NotNull String contractCode) throws IOException,
      InterruptedException {
    return Pool.executorService.submit(() -> {
      File temp = File.createTempFile("contract", ".sl");

      temp.deleteOnExit();

      BufferedWriter out = new BufferedWriter(new FileWriter(temp));
      out.write(contractCode);
      out.close();

      Process p = Runtime.getRuntime().exec("solc --binary stdout --optimize 1 " + temp.getPath());
      p.waitFor();

      java.util.Scanner s = new java.util.Scanner(p.getInputStream()).useDelimiter("\n");
      // Discard two lines
        s.next();
        s.next();
        // Keep the third line
        return s.next();
      });
  }
}
