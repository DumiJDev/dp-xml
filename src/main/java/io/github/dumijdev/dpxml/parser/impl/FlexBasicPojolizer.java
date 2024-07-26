package io.github.dumijdev.dpxml.parser.impl;

import io.github.dumijdev.dpxml.model.Node;
import io.github.dumijdev.dpxml.model.XMLNode;
import io.github.dumijdev.dpxml.parser.Nodilizer;
import io.github.dumijdev.dpxml.parser.Pojolizer;
import io.github.dumijdev.dpxml.stereotype.FlexElement;
import io.github.dumijdev.dpxml.stereotype.IgnoreElement;

public class FlexBasicPojolizer implements Pojolizer {
  private final ThreadLocal<Pojolizer> basic = ThreadLocal.withInitial(BasicPojolizer::new);
  private final ThreadLocal<Nodilizer> nodilizer = ThreadLocal.withInitial(DefaultNodilizer::new);

  @Override
  public <T> T pojoify(String xml, Class<T> clazz) throws Exception {

    var node = nodilizer.get().nodify(xml);

    return pojoify(node, clazz);
  }

  @Override
  public <T> T pojoify(Node node, Class<T> clazz) throws Exception {
    var outNode = new XMLNode(node.name());

    for (var field : clazz.getDeclaredFields()) {
      if(field.isAnnotationPresent(IgnoreElement.class))
        continue;

      if (field.isAnnotationPresent(FlexElement.class)) {
        var metadata = field.getAnnotation(FlexElement.class);

        var paths = metadata.path().split("[.]");
        Node temp = node;
        for (var path : paths) {
          temp = temp.child(path);
        }

        node.addChild(temp.name(), temp);
      }
    }

    return basic.get().pojoify(outNode, clazz);
  }
}
