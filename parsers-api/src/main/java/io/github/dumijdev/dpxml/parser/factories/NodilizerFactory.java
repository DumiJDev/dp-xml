package io.github.dumijdev.dpxml.parser.factories;

import io.github.dumijdev.dpxml.parser.api.AbstractNodilizer;

import java.util.ServiceLoader;

public abstract class NodilizerFactory {
  private NodilizerFactory() {
  }

  public static AbstractNodilizer find() {
    return ServiceLoader.load(AbstractNodilizer.class)
        .findFirst().orElseThrow(RuntimeException::new);
  }
}
