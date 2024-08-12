package io.github.dumijdev.dpxml.model;

import java.util.*;
import java.util.stream.Collectors;

public class XMLNode implements Node {
  private final Map<String, Node.Attribute> attributes = new HashMap<>();
  private final Map<String, List<Node>> children = new HashMap<>();
  private String name;
  private Node parent;
  private String content;
  private String namespace;

  public XMLNode() {

  }

  public XMLNode(Node parent) {
    this.parent = parent;
  }

  public XMLNode(String name) {
    this.name = name;
  }

  public XMLNode(String name, String namespace) {
    this.name = name;
    this.namespace = namespace;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Node parent() {
    return parent;
  }

  @Override
  public List<Node> children() {
    var nodes = new LinkedList<Node>();
    for (var child : this.children.values()) {
      nodes.addAll(child);
    }

    return nodes;
  }

  @Override
  public Node addChild(String name, Node node) {

    var oldValues = children.getOrDefault(name, new LinkedList<>());

    oldValues.add(node);

    children.put(name, oldValues);

    return this;
  }

  @Override
  public Node child(String name) {
    return children.getOrDefault(name, List.of(new XMLNode(this))).get(0);
  }

  @Override
  public Attribute attribute(String name) {
    return attributes.get(name);
  }

  @Override
  public Node addAttribute(Attribute attribute) {

    attributes.put(attribute.name(), attribute);

    return this;
  }

  @Override
  public Node addAttribute(String name, String value) {
    attributes.put(name, new XMLAttribute(name, value));

    return this;
  }

  @Override
  public List<Attribute> attributes() {
    return new ArrayList<>(attributes.values());
  }

  @Override
  public boolean isMissing() {
    return Objects.isNull(name);
  }

  @Override
  public String content() {
    return content;
  }

  @Override
  public List<Node> children(String name) {
    return children.getOrDefault(name, new ArrayList<>());
  }

  @Override
  public String namespace() {
    return namespace;
  }

  @Override
  public String asXml() {
    var builder = new StringBuilder();

    builder.append('<');
    if (Objects.nonNull(namespace()) && !namespace().isEmpty()) {
      builder.append(namespace()).append(':');
    }

    builder.append(name());

    for (var attribute : attributes()) {
      builder.append(' ').append(attribute.name()).append('=')
              .append("\"").append(attribute.value()).append("\"");
    }

    builder.append('>');

    if (Objects.nonNull(content()) && children.isEmpty()) {
      builder.append(content);
    }

    for (var child : children()) {
      builder.append(child.asXml());
    }

    builder.append("</");
    if (Objects.nonNull(namespace()) && !namespace().isEmpty()) {
      builder.append(namespace()).append(':');
    }

    builder.append(name());
    builder.append('>');

    return builder.toString();
  }

  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    XMLNode xmlNode = (XMLNode) o;

    if (!attributes.equals(xmlNode.attributes)) return false;
    if (!children.equals(xmlNode.children)) return false;
    if (!Objects.equals(name, xmlNode.name)) return false;
    return Objects.equals(parent, xmlNode.parent);
  }

  @Override
  public int hashCode() {
    int result = attributes.hashCode();
    result = 31 * result + children.hashCode();
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (parent != null ? parent.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "XMLNode{" +
        "attributes=" + attributes +
        ", children=" + children +
        ", name='" + name + '\'' +
        ", content='" + content + '\'' +
        "}";
  }
}
