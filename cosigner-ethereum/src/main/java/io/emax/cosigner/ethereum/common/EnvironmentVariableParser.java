package io.emax.cosigner.ethereum.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnvironmentVariableParser {
  /**
   * Resolves environment variables in strings.
   * 
   * @param input String that may contain environment variables.
   * @return String with variables resolved and expanded to their full value.
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
