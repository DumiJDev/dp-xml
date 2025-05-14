package io.github.dumijdev.dpxml.parser.api;

import io.github.dumijdev.dpxml.parser.model.Node;
import io.github.dumijdev.dpxml.parser.utils.XMLReaderContext.DocumentReader;
import org.w3c.dom.Element;


public interface Nodilizer {
  Node nodify(String xml);
}
