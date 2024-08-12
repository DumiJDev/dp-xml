package io.github.dumijdev.dpxml.parser.impl.xml;

import io.github.dumijdev.dpxml.model.Node;
import io.github.dumijdev.dpxml.parser.Xmlizer;

public class BasicXmlizer implements Xmlizer {
    @Override
    public String xmlify(Object xml) {
        return "";
    }

    @Override
    public String xmlify(Node node) {
        return node.asXml();
    }
}
