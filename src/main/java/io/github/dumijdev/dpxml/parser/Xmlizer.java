package io.github.dumijdev.dpxml.parser;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;

public interface Xmlizer {
    default Document convertToDoc(Object xml) throws Exception {
        return DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(xmlify(xml));
    }

    String xmlify(Object xml) throws Exception;
}
