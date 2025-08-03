package io.github.dumijdev.dpxml.parser.api;

import io.github.dumijdev.dpxml.parser.serializer.XmlSerializer;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractXmlizer implements Xmlizer {
  private final Map<Class<?>, XmlSerializer<?>> serializers = new HashMap<>();

  public AbstractXmlizer() {}

  protected XmlSerializer<?> getSerializer(Class<?> clazz) {
    return serializers.computeIfAbsent(clazz, aClass -> {
      try {
        var constructor = aClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return (XmlSerializer<?>) constructor.newInstance();
      } catch (Exception e) {
        throw new RuntimeException("Failed to instantiate serializer: " + aClass.getName(), e);
      }
    });
  }

  public void addSerializer(Class<?> clazz, XmlSerializer<?> xmlSerializer) {
    this.serializers.put(clazz, xmlSerializer);
  }
}
