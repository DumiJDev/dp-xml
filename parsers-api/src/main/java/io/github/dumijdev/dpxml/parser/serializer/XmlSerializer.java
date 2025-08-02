package io.github.dumijdev.dpxml.parser.serializer;

public interface XmlSerializer<T> {
  String serialize(T t);
}
