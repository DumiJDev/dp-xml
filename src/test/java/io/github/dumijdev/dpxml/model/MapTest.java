package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.Xmlizable;

import java.util.Map;

@Xmlizable
public class MapTest {
  private Map<String, String> map = Map.of("key1", "value1", "key2", "value2");

  public Map<String, String> getMap() {
    return map;
  }
}
