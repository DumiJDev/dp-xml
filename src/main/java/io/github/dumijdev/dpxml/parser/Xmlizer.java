package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.model.Node;
import io.github.dumijdev.dpxml.utils.XMLReaderContext;
import org.w3c.dom.Document;

public interface Xmlizer {
  private Document convertToDoc(Object xml) throws Exception {
    return XMLReaderContext.readerContext.get().parse(xmlify(xml));
  }

  String xmlify(Object xml) throws Exception;

  String xmlify(Node node);
}
