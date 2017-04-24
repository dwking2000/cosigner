package io.emax.cosigner.common.crypto;

import io.emax.cosigner.common.Json;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class JsonTest {
  @Test
  public void testJson() {
    HashMap<String, String> testMap = new HashMap<>();
    testMap.put("Key1", "Val1");
    testMap.put("Key2", "Val2");
    String expectedResult = "{\"Key2\":\"Val2\",\"Key1\":\"Val1\"}";

    String testResult = Json.stringifyObject(Map.class, testMap);
    Assert.assertTrue("Json string does not appear to be correct",
        expectedResult.equalsIgnoreCase(testResult));

    Map mapResult = (Map) Json.objectifyString(Map.class, testResult);
    Assert.assertTrue("Value 1 is not what we expected",
        "Val1".equalsIgnoreCase((String) mapResult.get("Key1")));
  }
}
