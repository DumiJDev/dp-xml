package io.github.dumijdev.dpxml.parser.serializer;

public interface XmlDeserializer<T> {
  T deserialize(String xml);
}
