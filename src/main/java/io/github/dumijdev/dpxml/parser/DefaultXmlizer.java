package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.model.Xmlizable;
import io.github.dumijdev.dpxml.stereotype.*;

import java.util.*;

import static io.github.dumijdev.dpxml.utils.ParserUtils.isCollection;
import static io.github.dumijdev.dpxml.utils.ParserUtils.isPrimitive;
import static java.lang.String.format;

public class DefaultXmlizer implements Xmlizer {
    private final Map<String, String> namespaces = new HashMap<>();

    @Override
    public String xmlify(Object obj) throws Exception {
        return xmlify(obj, null, null);
    }

    private String xmlify(Object obj, String name, String namespace) throws Exception {
        var clazz = obj.getClass();

        if (!clazz.isAnnotationPresent(Xmlizable.class)) {
            throw new Exception("Não é possível converter em XML, class: (" + obj.getClass().getSimpleName() + ")");
        }

        var builder = new StringBuilder();
        String rootName = Objects.isNull(name) ? clazz.getSimpleName().toLowerCase() : name;

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
        } else if (clazz.isAnnotationPresent(DeclaredNamespaces.class)) {
            var aliases = clazz.getDeclaredAnnotation(DeclaredNamespaces.class);
            for (var alias : aliases.aliases()) {
                if (!namespaces.containsKey(alias)) {
                    continue;
                }

                builder.append(format(" xmlns:%s=\"%s\"",
                                alias,
                                namespaces.get(alias)
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

        Arrays.stream(clazz.getDeclaredFields()).parallel().forEach(field -> {
            try {
                if (field.isAnnotationPresent(IgnoreElement.class)) {
                    return;
                }

                field.setAccessible(true);

                String name1 = field.getName();

                if (field.isAnnotationPresent(Element.class)) {
                    var metadata = field.getAnnotation(Element.class);

                    name1 = metadata.name().isEmpty() ? name1 : metadata.name();

                    name1 = !(name1.isEmpty() || metadata.namespace().isEmpty()) ? String.format("%s:%s", metadata.namespace(), name1) : name1;
                }

                if (Objects.isNull(field.get(obj))) {
                    return;
                }

                if (isCollection(field.getType())) {
                    var values = (Collection<Object>) field.get(obj);

                    for (var el : values) {
                        if (Objects.isNull(el)) {
                            continue;
                        }

                        if (isPrimitive(el.getClass())) {
                            builder.append("<").append(name1).append(">")
                                    .append(el)
                                    .append("</").append(name1).append(">");
                        } else {
                            String ns = null;

                            if (field.isAnnotationPresent(Element.class)) {
                                ns = field.getAnnotation(Element.class).namespace().isEmpty() ? ns : field.getAnnotation(Element.class).namespace();
                            }

                            builder.append(xmlify(el, name1, ns));
                        }
                    }
                } else {
                    if (isPrimitive(field.getType())) {
                        builder.append("<").append(name1).append(">")
                                .append(field.get(obj))
                                .append("</").append(name1).append(">");
                    } else {
                        String ns = null;

                        if (field.isAnnotationPresent(Element.class)) {
                            ns = field.getAnnotation(Element.class).namespace().isEmpty() ? ns : field.getAnnotation(Element.class).namespace();
                        }

                        if (!Objects.isNull(ns) && name1.split(":").length > 1) {
                            name1 = name1.split(":")[1];
                        }

                        builder.append(xmlify(field.get(obj), name1, ns));
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        return builder.append("</")
                .append(rootName)
                .append(">")
                .toString();
    }

    public DefaultXmlizer registerNamespace(String name, String value) {
        namespaces.put(name, value);

        return this;
    }
}
