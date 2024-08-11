package io.github.dumijdev.dpxml.model;

import java.util.List;

public interface Node {
  String name();

  Node parent();

  List<Node> children();

  Node addChild(String name, Node node);

  Node child(String name);

  Attribute attribute(String name);

  Node addAttribute(Attribute attribute);

  Node addAttribute(String name, String value);

  List<Attribute> attributes();

  boolean isMissing();

  String content();

  List<Node> children(String name);

  String namespace();

  String asXml();

  interface Attribute {
    String name();

    String value();
  }

}
