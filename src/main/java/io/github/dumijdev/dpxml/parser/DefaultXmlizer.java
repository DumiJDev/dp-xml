package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.model.Xmlizable;
import io.github.dumijdev.dpxml.stereotype.Element;
import io.github.dumijdev.dpxml.stereotype.IgnoreElement;
import io.github.dumijdev.dpxml.stereotype.Namespaces;
import io.github.dumijdev.dpxml.stereotype.RootElement;

import java.util.Collection;
import java.util.Objects;

import static io.github.dumijdev.dpxml.utils.ParserUtils.isCollection;
import static io.github.dumijdev.dpxml.utils.ParserUtils.isPrimitive;
import static java.lang.String.format;

public class DefaultXmlizer implements Xmlizer {
    @Override
    public String xmlify(Object obj) throws Exception {
        var clazz = obj.getClass();

        if (!clazz.isAnnotationPresent(Xmlizable.class)) {
            throw new Exception("Não é possível converter em XML, class: (" + obj.getClass().getSimpleName() + ")");
        }

        var builder = new StringBuilder();
        String rootName = null;

        if (clazz.isAnnotationPresent(RootElement.class)) {
            var metadata = clazz.getDeclaredAnnotation(RootElement.class);
            rootName = metadata.name().isEmpty() ? clazz.getSimpleName().toLowerCase() : metadata.name();

            rootName = metadata.namespace().isEmpty() ? rootName : format("%s:%s", metadata.namespace(), rootName);


        } else {
            rootName = clazz.getSimpleName().toLowerCase();
        }

        builder.append("<").append(rootName);

        if (clazz.isAnnotationPresent(Namespaces.class)) {
            var namespaces = clazz.getDeclaredAnnotation(Namespaces.class);

            for (var namespace : namespaces.namespaces()) {
                if (namespace.name().isEmpty() && namespace.value().isEmpty()) {
                    continue;
                }

                builder.append(format(" xmlns:%s=\"%s\"",
                                namespace.name(),
                                namespace.value()
                        )
                );
            }
        }

        builder.append(">");

        for (var field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(IgnoreElement.class)) {
                continue;
            }

            field.setAccessible(true);

            String name = "";

            if (field.isAnnotationPresent(Element.class)) {
                var metadata = field.getAnnotation(Element.class);

                name = metadata.name().isEmpty() ? name : metadata.name();

                name = !(name.isEmpty() || metadata.namespace().isEmpty()) ? String.format("%s:%s", metadata.namespace(), name) : name;
            }

            name = name.isEmpty() ? field.getName() : name;

            if (Objects.isNull(field.get(obj))) {
                continue;
            }

            if (isCollection(field.getType())) {
                var values = (Collection<Object>) field.get(obj);

                for (var el : values) {
                    if (Objects.isNull(el)) {
                        continue;
                    }

                    if (isPrimitive(el.getClass())) {
                        builder.append("<").append(name).append(">")
                                .append(el)
                                .append("</").append(name).append(">");
                    } else {
                        builder.append(xmlify(el));
                    }
                }
            } else {
                if (isPrimitive(field.getType())) {
                    builder.append("<").append(name).append(">")
                            .append(field.get(obj))
                            .append("</").append(name).append(">");
                } else {
                    builder.append(xmlify(field.get(obj)));
                }


            }

        }

        return builder.append("</")
                .append(rootName)
                .append(">")
                .toString();
    }
}
