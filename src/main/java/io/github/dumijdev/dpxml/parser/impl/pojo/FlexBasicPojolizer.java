package io.github.dumijdev.dpxml.parser.impl.pojo;

import io.github.dumijdev.dpxml.model.Node;
import io.github.dumijdev.dpxml.parser.Nodilizer;
import io.github.dumijdev.dpxml.parser.Pojolizer;
import io.github.dumijdev.dpxml.parser.impl.node.DefaultNodilizer;
import io.github.dumijdev.dpxml.stereotype.FlexElement;
import io.github.dumijdev.dpxml.stereotype.IgnoreElement;

public class FlexBasicPojolizer implements Pojolizer {
    private final ThreadLocal<Pojolizer> basic = ThreadLocal.withInitial(BasicPojolizer::new);
    private final ThreadLocal<Nodilizer> nodilizer = ThreadLocal.withInitial(DefaultNodilizer::new);

    @Override
    @SuppressWarnings("unchecked")
    public <T> T pojoify(String xml, Class<T> clazz) {
        if (String.class.equals(clazz)) {

            return (T) xml;
        }

        var node = nodilizer.get().nodify(xml);

        return pojoify(node, clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T pojoify(Node node, Class<T> clazz) {
        if (String.class.equals(clazz)) {

            return (T) node.asXml();
        }

        for (var field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(IgnoreElement.class))
                continue;

            if (field.isAnnotationPresent(FlexElement.class)) {
                var metadata = field.getAnnotation(FlexElement.class);

                var paths = metadata.src().isEmpty() ? field.getName().split("[.]", 1) : metadata.src().split("[.]");
                Node temp = node;

                for (var i = 0; i < paths.length - 1; i++) {
                    temp = temp.child(paths[i]);
                }

                var name = metadata.dst().isEmpty() ? field.getName() : metadata.dst();

                for (var child : temp.children(paths[paths.length - 1])) {
                    node.addChild(name, child);
                }
            }
        }

        return basic.get().pojoify(node, clazz);
    }
}
