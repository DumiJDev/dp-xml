package io.github.dumijdev.dpxml.parser.api;

import org.w3c.dom.NodeList;

public interface XPathAdapter<T> {
  T adapt(NodeList nodes) throws Exception;

  class DefaultAdapter implements XPathAdapter<Object> {
    public Object adapt(NodeList nodes) {
      return nodes.item(0).getTextContent();
    }
  }
}
