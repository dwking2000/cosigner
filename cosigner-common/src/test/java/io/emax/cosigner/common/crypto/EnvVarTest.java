package io.emax.cosigner.common.crypto;

import io.emax.cosigner.common.EnvironmentVariableParser;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class EnvVarTest {
  @Test
  public void testEnvVar() {
    HashMap<String, String> fakeEnv = new HashMap<>();
    fakeEnv.put("SHELL", "/bin/sh");
    String tester = "${SHELL}";
    tester = EnvironmentVariableParser.resolveEnvVars(tester, fakeEnv);
    Assert.assertTrue("Failed to parse environment variables", "/bin/sh".equalsIgnoreCase(tester));
  }
}
