package io.github.dumijdev.dpxml.parser.factories;

import io.github.dumijdev.dpxml.parser.api.AbstractXmlizer;
import io.github.dumijdev.dpxml.parser.api.Xmlizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

public abstract class XmlizerFactory {
  private static final Logger logger = LoggerFactory.getLogger(XmlizerFactory.class);
  private XmlizerFactory() {
  }

  public static AbstractXmlizer find() {
    return ServiceLoader.load(AbstractXmlizer.class)
        .findFirst().orElseThrow(RuntimeException::new);
  }
}
