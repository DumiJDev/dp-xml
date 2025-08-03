package io.github.dumijdev.dpxml.parser.api;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

public interface Pojolizer {
  default <T> T pojoify(String xml, Class<T> clazz) {
    return pojoify(new StringReader(xml), clazz);
  }

  default <T> T pojoify(byte[] xml, Class<T> clazz) {
    return pojoify(new String(xml), clazz);
  }

  default <T> T pojoify(InputStream xml, Class<T> clazz) {
    return pojoify(new InputStreamReader(xml), clazz);
  }

  <T> T pojoify(Reader xml, Class<T> clazz);

}
