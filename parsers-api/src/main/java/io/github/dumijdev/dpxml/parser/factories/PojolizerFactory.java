package io.github.dumijdev.dpxml.parser.factories;

import io.github.dumijdev.dpxml.parser.api.AbstractPojolizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public abstract class PojolizerFactory {
  private static final Logger logger = LoggerFactory.getLogger(PojolizerFactory.class);
  private PojolizerFactory() {
  }

  public static AbstractPojolizer find() {
    var items = ServiceLoader.load(AbstractPojolizer.class);

    if (items.stream().count() > 1) {
      logger.warn("More than one Pojolizer found. Using the first one.");
      logger.warn("Pojolizers found: {}", items.stream().collect(toList()));
      logger.info("First pojolizer will be used: {}", items.stream().findFirst().orElse(null));
    }

    return items.findFirst().orElseThrow(RuntimeException::new);
  }
}
