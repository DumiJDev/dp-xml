package io.github.dumijdev.dpxml.parser.api;

import io.github.dumijdev.dpxml.parser.model.Node;
import io.github.dumijdev.dpxml.parser.utils.XMLReaderContext;
import org.w3c.dom.Document;

public interface Xmlizer {
  private Document convertToDoc(Object xml) throws Exception {
    return XMLReaderContext.readerContext.get().parse(xmlify(xml));
  }

  String xmlify(Object xml);

  String xmlify(Node node);
}
