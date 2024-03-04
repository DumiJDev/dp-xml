package io.github.dumijdev.dpxml.parser;

@FunctionalInterface
public interface Pojolizer {
    <T> T pojoify(String xml, Class<T> clazz) throws Exception;
}
