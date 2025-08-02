package io.github.dumijdev.dpxml.parser.api;

import io.github.dumijdev.dpxml.parser.model.Node;
import io.github.dumijdev.dpxml.parser.serializer.XmlDeserializer;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractPojolizer implements Pojolizer {
  private final Map<Class<?>, XmlDeserializer<?>> deserializers = new HashMap<>();

  public AbstractPojolizer() {}

  protected XmlDeserializer<?> getDeserializer(Class<?> clazz) {
    return deserializers.get(clazz);
  }

  public AbstractPojolizer addDeserializer(Class<?> clazz, XmlDeserializer<?> xmlDeserializer) {
    this.deserializers.put(clazz, xmlDeserializer);
    return this;
  }

  @Override
  public <T> T pojoify(Node node, Class<T> clazz) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
