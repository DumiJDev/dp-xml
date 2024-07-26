package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.model.Node;
import io.github.dumijdev.dpxml.model.XMLAttribute;
import io.github.dumijdev.dpxml.model.XMLNode;
import io.github.dumijdev.dpxml.utils.XMLreaderContext;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Objects;

import static io.github.dumijdev.dpxml.utils.ParserUtils.simpleNodeName;

public interface Nodilizer {
  private Node fromElement(Element element) {
    var node = new XMLNode(simpleNodeName(element.getNodeName()));
    node.setContent(element.getTextContent());
    var children = element.getChildNodes();
    for (var i = 0; i < children.getLength(); i++) {
      var item = children.item(i);
      if (item.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        var child = fromElement((org.w3c.dom.Element) item);
        node.addChild(child.name(), child);
      }

      var attributes = item.getAttributes();
      if (Objects.nonNull(attributes))
        for (var j = 0; j < attributes.getLength(); j++) {
          var attrib = attributes.item(j);

          node.addAttribute(new XMLAttribute(attrib.getNodeName(), attrib.getTextContent()));
        }
    }

    return node;
  }

  default Node nodify(String xml) {
    try {
      Element element = XMLreaderContext.readerContext.get().parse(new InputSource(new StringReader(xml))).getDocumentElement();

      return fromElement(element);
    } catch (SAXException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
