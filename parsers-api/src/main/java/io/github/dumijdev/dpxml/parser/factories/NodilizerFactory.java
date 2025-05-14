package io.github.dumijdev.dpxml.parser.factories;

import io.github.dumijdev.dpxml.parser.api.Nodilizer;

import java.util.ServiceLoader;

public abstract class NodilizerFactory {
  private NodilizerFactory() {}

  public Nodilizer getNodilizer() {
    return ServiceLoader.load(Nodilizer.class)
        .findFirst().orElseThrow(RuntimeException::new);
  }
}
