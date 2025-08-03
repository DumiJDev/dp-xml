package io.github.dumijdev.dpxml.parser.utils;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Handles XML element reading operations
 */
public class XmlElementReader {

  public String readElementText(XMLStreamReader reader) throws XMLStreamException {
    StringBuilder text = new StringBuilder();

    while (reader.hasNext()) {
      int event = reader.next();

      switch (event) {
        case XMLStreamConstants.CHARACTERS:
        case XMLStreamConstants.CDATA:
          text.append(reader.getText());
          break;
        case XMLStreamConstants.END_ELEMENT:
          return text.toString().trim();
      }
    }

    return text.toString().trim();
  }

  public String captureElementText(XMLStreamReader reader) throws XMLStreamException {
    StringBuilder text = new StringBuilder();
    int depth = 1;

    while (reader.hasNext() && depth > 0) {
      int event = reader.next();

      switch (event) {
        case XMLStreamConstants.START_ELEMENT:
          depth++;
          appendStartElement(text, reader);
          break;
        case XMLStreamConstants.END_ELEMENT:
          depth--;
          if (depth > 0) {
            appendEndElement(text, reader);
          }
          break;
        case XMLStreamConstants.CHARACTERS:
        case XMLStreamConstants.CDATA:
          text.append(reader.getText());
          break;
      }
    }

    return text.toString();
  }

  private void appendStartElement(StringBuilder text, XMLStreamReader reader) {
    text.append("<").append(reader.getLocalName()).append(">");
  }

  private void appendEndElement(StringBuilder text, XMLStreamReader reader) {
    text.append("</").append(reader.getLocalName()).append(">");
  }
}
