package io.github.dumijdev.dpxml.utils;

import io.github.dumijdev.dpxml.exception.UnParsebleException;
import io.github.dumijdev.dpxml.exception.UnPojolizableException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

public class XMLReaderContext {
  public static ThreadLocal<DocumentBuilder> readerContext = ThreadLocal.withInitial(() -> {
    try {
      return DocumentBuilderFactory.newInstance().newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  });

  public static class DocumentReader {
    public static Document read(String xml) {
        try {
            return readerContext.get().parse(new InputSource(new StringReader(xml)));
        } catch (SAXException | IOException e) {
            throw new UnParsebleException(e);
        }
    }
  }
}
