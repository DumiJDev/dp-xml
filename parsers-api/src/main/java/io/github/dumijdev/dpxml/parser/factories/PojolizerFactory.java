package io.github.dumijdev.dpxml.parser.factories;

import io.github.dumijdev.dpxml.parser.api.Pojolizer;

import java.util.ServiceLoader;

public abstract class PojolizerFactory {
  private PojolizerFactory() {}

  public static Pojolizer getPojolizer() {
    return ServiceLoader.load(Pojolizer.class)
        .findFirst().orElseThrow(RuntimeException::new);
  }
}
