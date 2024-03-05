package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.model.Xmlizable;
import io.github.dumijdev.dpxml.stereotype.*;

import java.util.Collection;
import java.util.Objects;

import static io.github.dumijdev.dpxml.utils.ParserUtils.isCollection;
import static io.github.dumijdev.dpxml.utils.ParserUtils.isPrimitive;
import static java.lang.String.format;

public class DefaultXmlizer implements Xmlizer {
    @Override
    public String xmlify(Object obj) throws Exception {
        return xmlify(obj, null);
    }

    private String xmlify(Object obj, String namespace) throws Exception {
        var clazz = obj.getClass();

        if (!clazz.isAnnotationPresent(Xmlizable.class)) {
            throw new Exception("Não é possível converter em XML, class: (" + obj.getClass().getSimpleName() + ")");
        }

        var builder = new StringBuilder();
        String rootName = clazz.getSimpleName().toLowerCase();

        if (clazz.isAnnotationPresent(RootElement.class) && Objects.isNull(namespace)) {
            var metadata = clazz.getDeclaredAnnotation(RootElement.class);
            rootName = metadata.name().isEmpty() ? rootName : metadata.name();

            rootName = metadata.namespace().isEmpty() ? rootName : format("%s:%s", metadata.namespace(), rootName);

        } else if (!Objects.isNull(namespace)) {
            rootName = String.format("%s:%s", namespace, rootName);
        }

        builder.append("<").append(rootName);

        if (clazz.isAnnotationPresent(Namespaces.class)) {
            var namespaces = clazz.getDeclaredAnnotation(Namespaces.class);

            for (var namespace1 : namespaces.namespaces()) {
                if (namespace1.name().isEmpty() && namespace1.value().isEmpty()) {
                    continue;
                }

                builder.append(format(" xmlns:%s=\"%s\"",
                                namespace1.name(),
                                namespace1.value()
                        )
                );
            }
        }

        if (clazz.isAnnotationPresent(Namespace.class)) {
            var namespace1 = clazz.getDeclaredAnnotation(Namespace.class);

            if (!namespace1.name().isEmpty() && !namespace1.value().isEmpty()) {
                builder.append(format(" xmlns:%s=\"%s\"",
                                namespace1.name(),
                                namespace1.value()
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

            String name = field.getName();

            if (field.isAnnotationPresent(Element.class)) {
                var metadata = field.getAnnotation(Element.class);

                name = metadata.name().isEmpty() ? name : metadata.name();

                name = !(name.isEmpty() || metadata.namespace().isEmpty()) ? String.format("%s:%s", metadata.namespace(), name) : name;
            }

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
                        String ns = null;

                        if (field.isAnnotationPresent(Element.class)) {
                            ns = field.getAnnotation(Element.class).namespace().isEmpty() ? ns : field.getAnnotation(Element.class).namespace();
                        }

                        builder.append(xmlify(el, ns));
                    }
                }
            } else {
                if (isPrimitive(field.getType())) {
                    builder.append("<").append(name).append(">")
                            .append(field.get(obj))
                            .append("</").append(name).append(">");
                } else {
                    String ns = null;

                    if (field.isAnnotationPresent(Element.class)) {
                        ns = field.getAnnotation(Element.class).namespace().isEmpty() ? ns : field.getAnnotation(Element.class).namespace();
                    }

                    builder.append(xmlify(field.get(obj), ns));
                }
            }
        }

        return builder.append("</")
                .append(rootName)
                .append(">")
                .toString();
    }
}
