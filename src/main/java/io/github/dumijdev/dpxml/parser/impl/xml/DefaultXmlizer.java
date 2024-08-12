package io.github.dumijdev.dpxml.parser.impl.xml;

import io.github.dumijdev.dpxml.exception.InternalErrorException;
import io.github.dumijdev.dpxml.exception.UnXmlizableException;
import io.github.dumijdev.dpxml.model.Node;
import io.github.dumijdev.dpxml.parser.Xmlizer;
import io.github.dumijdev.dpxml.stereotype.*;
import io.github.dumijdev.dpxml.utils.Attributes;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static io.github.dumijdev.dpxml.utils.ParserUtils.isCollection;
import static io.github.dumijdev.dpxml.utils.ParserUtils.isPrimitive;
import static java.lang.String.format;

public class DefaultXmlizer implements Xmlizer {
    private final Map<String, String> namespaces = new HashMap<>();

    @Override
    public String xmlify(Object obj) {
        return xmlify(obj, null, null);
    }

    @Override
    public String xmlify(Node node) {
        return node.asXml();
    }

    private String xmlify(Object obj, String name, String namespace) {
        try {
            var clazz = obj.getClass();

            if (!clazz.isAnnotationPresent(Xmlizable.class)) {
                throw new UnXmlizableException(obj.getClass().getSimpleName());
            }

            var builder = new StringBuilder();
            var tempBuilder = new StringBuilder();
            var rootName = getName(name, namespace, clazz);
            var attributes = Attributes.getAttributes(clazz, obj);

            var rootNameTemp = processAttributes(rootName, clazz.getName(), attributes);

            tempBuilder.append(rootNameTemp);

            processNamespaces(clazz, tempBuilder);

            openField(builder, tempBuilder.toString());

            Arrays.stream(clazz.getDeclaredFields()).forEach(field -> {
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
                                var name1Temp = processAttributes(name1, field.getName(), attributes);
                                openField(builder, name1Temp);
                                builder.append(el);
                                closeField(builder, name1);
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
                            var name1Temp = processAttributes(name1, field.getName(), attributes);
                            openField(builder, name1Temp);
                            builder.append(field.get(obj));
                            closeField(builder, name1);
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

            closeField(builder, rootName);

            return builder.toString();
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new InternalErrorException(e);
        }
    }

    private String processAttributes(String fieldName, String key, Map<String, String> attributes) {
        if (!attributes.containsKey(key)) {
            return fieldName;
        }

        return String.format("%s %s", fieldName, attributes.get(key));
    }

    private void processNamespaces(Class<?> clazz, StringBuilder tempBuilder) {
        if (clazz.isAnnotationPresent(Namespaces.class)) {
            var namespaces = clazz.getDeclaredAnnotation(Namespaces.class);

            for (var namespace1 : namespaces.namespaces()) {
                if (namespace1.name().isEmpty() && namespace1.value().isEmpty()) {
                    continue;
                }

                tempBuilder.append(format(" xmlns:%s=\"%s\"",
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

                tempBuilder.append(format(" xmlns:%s=\"%s\"",
                                alias,
                                namespaces.get(alias)
                        )
                );
            }
        }

        if (clazz.isAnnotationPresent(Namespace.class)) {
            var namespace1 = clazz.getDeclaredAnnotation(Namespace.class);

            if (!namespace1.name().isEmpty() && !namespace1.value().isEmpty()) {
                tempBuilder.append(format(" xmlns:%s=\"%s\"",
                                namespace1.name(),
                                namespace1.value()
                        )
                );
            }
        }

    }

    private void openField(StringBuilder builder, String name) {
        builder.append("<").append(name).append(">");
    }

    private void closeField(StringBuilder builder, String name) {
        builder.append("</").append(name).append(">");
    }

    private String getName(String name, String namespace, Class<?> clazz) {
        String rootName = Objects.isNull(name) ? clazz.getSimpleName().toLowerCase() : name;

        if (clazz.isAnnotationPresent(RootElement.class) && Objects.isNull(namespace)) {
            var metadata = clazz.getDeclaredAnnotation(RootElement.class);
            rootName = metadata.name().isEmpty() ? rootName : metadata.name();

            rootName = metadata.namespace().isEmpty() ? rootName : format("%s:%s", metadata.namespace(), rootName);

        } else if (!Objects.isNull(namespace)) {
            rootName = String.format("%s:%s", namespace, rootName);
        }
        return rootName;
    }

    public DefaultXmlizer registerNamespace(String name, String value) {
        if ((Objects.isNull(name) || name.isEmpty()) || (Objects.isNull(value) || value.isEmpty())) return this;

        namespaces.put(name, value);

        return this;
    }

    public DefaultXmlizer registerNamespaces(Map<String, String> namespaces) {
        namespaces.forEach(this::registerNamespace);

        return this;
    }
}
