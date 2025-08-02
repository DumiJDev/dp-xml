package io.github.dumijdev.dpxml.parser.api;

import io.github.dumijdev.dpxml.parser.serializer.XmlSerializer;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractXmlizer implements Xmlizer {
  private final Map<Class<?>, XmlSerializer<?>> serializers = new HashMap<>();

  public AbstractXmlizer() {}

  protected XmlSerializer<?> getSerializer(Class<?> clazz) {
    return serializers.get(clazz);
  }

  public AbstractXmlizer addSerializer(Class<?> clazz, XmlSerializer<?> xmlSerializer) {
    this.serializers.put(clazz, xmlSerializer);
    return this;
  }
}
