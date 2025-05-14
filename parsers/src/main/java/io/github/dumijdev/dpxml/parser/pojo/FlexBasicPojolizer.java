package io.github.dumijdev.dpxml.parser.pojo;

import io.github.dumijdev.dpxml.parser.api.Nodilizer;
import io.github.dumijdev.dpxml.parser.api.Pojolizer;
import io.github.dumijdev.dpxml.parser.exception.UnPojolizableException;
import io.github.dumijdev.dpxml.parser.model.Node;
import io.github.dumijdev.dpxml.parser.node.DefaultNodilizer;
import io.github.dumijdev.dpxml.annotations.FlexElement;
import io.github.dumijdev.dpxml.annotations.IgnoreElement;
import io.github.dumijdev.dpxml.annotations.Pojolizable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FlexBasicPojolizer implements Pojolizer {
  private final ThreadLocal<Pojolizer> basic = ThreadLocal.withInitial(BasicPojolizer::new);
  private final ThreadLocal<Nodilizer> nodilizer = ThreadLocal.withInitial(DefaultNodilizer::new);

  private void clear() {
    basic.remove();
    nodilizer.remove();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T pojoify(String xml, Class<T> clazz) {
    if (String.class.equals(clazz)) {
      return (T) xml;
    }

    validateClass(clazz);

    var node = nodilizer.get().nodify(xml);

    return pojoify(node, clazz);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T pojoify(Node node, Class<T> clazz) {
    if (String.class.equals(clazz)) {
      return (T) node.asXml();
    }

    validateClass(clazz);

    for (var field : clazz.getDeclaredFields()) {
      if (field.isAnnotationPresent(IgnoreElement.class))
        continue;

      if (field.isAnnotationPresent(FlexElement.class)) {
        var metadata = field.getAnnotation(FlexElement.class);

        var paths = metadata.src().isEmpty() ? new String[]{field.getName()} : metadata.src().split("\\.");
        var name = metadata.dst().isEmpty() ? field.getName() : metadata.dst();

        flattenTree(node, Arrays.asList(paths), name);
      }
    }

    var pojo = basic.get().pojoify(node, clazz);

    clear();

    return pojo;
  }

  private void flattenTree(Node root, List<String> path, String name) {
    List<Node> foundNodes = findNodes(root, path, 0);
    for (Node node : foundNodes) {
      root.addChild(name, node);
    }
  }

  private List<Node> findNodes(Node node, List<String> path, int index) {
    if (index >= path.size()) return Collections.emptyList();
    List<Node> foundNodes = new ArrayList<>();

    if (node.name().equals(path.get(index))) {
      if (index == path.size() - 1) {
        foundNodes.add(node);
      } else {
        for (Node child : node.children()) {
          foundNodes.addAll(findNodes(child, path, index + 1));
        }
      }
    } else {
      for (Node child : node.children()) {
        foundNodes.addAll(findNodes(child, path, index));
      }
    }

    return foundNodes;
  }

  private <T> void validateClass(Class<T> clazz) {
    if (clazz == null) {
      throw new UnPojolizableException();
    }

    if (!clazz.isAnnotationPresent(Pojolizable.class)) {
      throw new UnPojolizableException(clazz.getSimpleName());
    }
  }

}
