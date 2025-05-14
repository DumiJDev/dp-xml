package io.github.dumijdev.dpxml.parser.factories;

import io.github.dumijdev.dpxml.parser.api.Xmlizer;

import java.util.ServiceLoader;

public abstract class XmlizerFactory {
  private XmlizerFactory() {}

  public static Xmlizer getXmlizer() {
    return ServiceLoader.load(Xmlizer.class)
        .findFirst().orElseThrow(RuntimeException::new);
  }
}
