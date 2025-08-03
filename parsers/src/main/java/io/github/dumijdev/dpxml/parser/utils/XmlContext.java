package io.github.dumijdev.dpxml.parser.utils;

/**
 * Immutable context for XML parsing state
 */
public class XmlContext {
  private final String currentPath;
  private final int depth;

  private XmlContext(String currentPath, int depth) {
    this.currentPath = currentPath;
    this.depth = depth;
  }

  public static XmlContext root() {
    return new XmlContext("", 0);
  }

  public XmlContext withPath(String path) {
    return new XmlContext(path, depth + 1);
  }

  String getCurrentPath() {
    return currentPath;
  }

  int getDepth() {
    return depth;
  }

}
