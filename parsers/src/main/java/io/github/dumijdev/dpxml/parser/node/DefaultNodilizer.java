package io.github.dumijdev.dpxml.parser.node;

import io.github.dumijdev.dpxml.parser.api.Nodilizer;
import io.github.dumijdev.dpxml.parser.model.Node;
import io.github.dumijdev.dpxml.parser.model.XMLAttribute;
import io.github.dumijdev.dpxml.parser.model.XMLNode;
import io.github.dumijdev.dpxml.parser.utils.XMLReaderContext;
import org.w3c.dom.Element;

import java.util.Objects;

import static io.github.dumijdev.dpxml.parser.utils.ParserUtils.simpleNodeName;

public class DefaultNodilizer implements Nodilizer {


  public DefaultNodilizer() {}

  private Node fromElement(Element element) {
    var node = new XMLNode(simpleNodeName(element.getNodeName()));
    node.setContent(element.getTextContent());
    var children = element.getChildNodes();
    for (var i = 0; i < children.getLength(); i++) {
      var item = children.item(i);
      if (item.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        var child = fromElement((Element) item);
        node.addChild(child.name(), child);
      }
    }

    var attributes = element.getAttributes();
    if (Objects.nonNull(attributes))
      for (var j = 0; j < attributes.getLength(); j++) {
        var attrib = attributes.item(j);

        node.addAttribute(new XMLAttribute(attrib.getNodeName(), attrib.getTextContent()));
      }

    return node;
  }

  @Override
  public Node nodify(String xml) {
    Element element = XMLReaderContext.DocumentReader.read(xml).getDocumentElement();

    return fromElement(element);
  }
}
