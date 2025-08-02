package io.github.dumijdev.dpxml.parser.factories;

import io.github.dumijdev.dpxml.parser.api.AbstractXmlizer;
import io.github.dumijdev.dpxml.parser.api.Xmlizer;

import java.util.ServiceLoader;

public abstract class XmlizerFactory {
  private XmlizerFactory() {
  }

  public static AbstractXmlizer find() {
    return ServiceLoader.load(AbstractXmlizer.class)
        .findFirst().orElseThrow(RuntimeException::new);
  }
}
