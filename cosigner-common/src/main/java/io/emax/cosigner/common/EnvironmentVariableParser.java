package io.emax.cosigner.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnvironmentVariableParser {

  /**
   * Parses a string, resolves and expands any environment variables.
   * 
   * @param input String to resolve
   * @return String with environment variables expanded to their actual values.
   */
  public static String resolveEnvVars(String input) {
    if (input == null) {
      return null;
    }

    Pattern pattern = Pattern.compile("\\$\\{(\\w+)\\}|\\$(\\w+)");
    Matcher matcher = pattern.matcher(input);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      String envVarName = null == matcher.group(1) ? matcher.group(2) : matcher.group(1);
      String envVarValue = System.getenv(envVarName);
      matcher.appendReplacement(buffer, null == envVarValue ? "" : envVarValue);
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }
}
