package io.github.dumijdev.dpxml.parser.api;


import io.github.dumijdev.dpxml.parser.model.Node;

public interface Pojolizer {
  <T> T pojoify(String xml, Class<T> clazz);

  <T> T pojoify(Node node, Class<T> clazz);
}
