package io.github.dumijdev.dpxml.parser;

@FunctionalInterface
public interface Pojolizer {
    <T> T convert(String xml, Class<T> clazz) throws Exception;
}
