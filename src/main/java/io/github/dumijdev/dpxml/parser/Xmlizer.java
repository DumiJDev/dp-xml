package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.model.Xmlizable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public interface Xmlizer {
    default Document convertToDoc(Xmlizable xml) throws ParserConfigurationException, IOException, SAXException {
        return DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(convertToString(xml));
    }

    String convertToString(Xmlizable xml);
}
