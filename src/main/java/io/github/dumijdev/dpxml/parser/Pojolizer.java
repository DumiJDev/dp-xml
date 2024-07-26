package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.model.Node;

public interface Pojolizer {
  <T> T pojoify(String xml, Class<T> clazz) throws Exception;

  <T> T pojoify(Node node, Class<T> clazz) throws Exception;
}
