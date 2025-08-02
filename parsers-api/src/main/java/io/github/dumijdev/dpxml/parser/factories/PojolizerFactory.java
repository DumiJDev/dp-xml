package io.github.dumijdev.dpxml.parser.factories;

import io.github.dumijdev.dpxml.parser.api.AbstractPojolizer;

import java.util.ServiceLoader;

public abstract class PojolizerFactory {
  private PojolizerFactory() {}

  public static AbstractPojolizer find() {
    return ServiceLoader.load(AbstractPojolizer.class)
        .findFirst().orElseThrow(RuntimeException::new);
  }
}
