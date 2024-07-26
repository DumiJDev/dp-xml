package io.github.dumijdev.dpxml.utils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XMLreaderContext {
  public static ThreadLocal<DocumentBuilder> readerContext = ThreadLocal.withInitial(() -> {
    try {
      return DocumentBuilderFactory.newInstance().newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  });
}
