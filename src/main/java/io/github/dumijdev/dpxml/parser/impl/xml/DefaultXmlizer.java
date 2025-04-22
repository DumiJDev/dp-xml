package io.github.dumijdev.dpxml.parser.impl.xml;

import io.github.dumijdev.dpxml.exception.InternalErrorException;
import io.github.dumijdev.dpxml.exception.UnXmlizableException;
import io.github.dumijdev.dpxml.model.Node;
import io.github.dumijdev.dpxml.parser.Xmlizer;
import io.github.dumijdev.dpxml.stereotype.*;
import io.github.dumijdev.dpxml.utils.Attributes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static io.github.dumijdev.dpxml.utils.ParserUtils.*;
import static java.lang.String.format;

public class DefaultXmlizer implements Xmlizer {
  private static final String NAMESPACE_FORMAT = "%s:%s";
  private static final String ATTRIBUTE_FORMAT = "%s %s";
  private static final String XMLNS_FORMAT = " xmlns:%s=\"%s\"";
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
    if (obj == null) {
      return "";
    }

    try {
      Class<?> clazz = obj.getClass();

      if (!clazz.isAnnotationPresent(Xmlizable.class) && !isMap(clazz)) {
        throw new UnXmlizableException(clazz.getSimpleName());
      }

      if (isMap(clazz)) {
        return xmlifyMap((Map<?, ?>) obj, namespace);
      }

      StringBuilder builder = new StringBuilder();
      Map<String, String> attributes = Attributes.getAttributes(clazz, obj);

      String rootName = determineRootName(name, namespace, clazz);
      String rootNameWithAttributes = addAttributesToElement(rootName, clazz.getName(), attributes);

      StringBuilder openTagBuilder = new StringBuilder(rootNameWithAttributes);
      addNamespaceDeclarations(clazz, openTagBuilder);

      appendOpenTag(builder, openTagBuilder.toString());
      processFields(obj, clazz, builder, attributes);
      appendCloseTag(builder, rootName);

      return builder.toString();
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new InternalErrorException(e);
    }
  }

  private void processFields(Object obj, Class<?> clazz, StringBuilder builder, Map<String, String> attributes) {
    Arrays.stream(clazz.getDeclaredFields())
        .filter(field -> !field.isAnnotationPresent(IgnoreElement.class))
        .forEach(field -> processField(field, obj, builder, attributes));
  }

  private void processField(Field field, Object obj, StringBuilder builder, Map<String, String> attributes) {
    try {
      field.setAccessible(true);
      Object fieldValue = field.get(obj);

      if (fieldValue == null) {
        return;
      }

      String fieldName = determineFieldName(field);

      if (isCollection(field.getType())) {
        processCollectionField(field, fieldName, (Collection<?>) fieldValue, builder, attributes);
      } else {
        processSingleField(field, fieldName, fieldValue, builder, attributes);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private void processCollectionField(Field field, String fieldName, Collection<?> values,
                                      StringBuilder builder, Map<String, String> attributes) {
    for (Object element : values) {
      if (element == null) {
        continue;
      }

      if (isPrimitive(element.getClass())) {
        String fieldNameWithAttributes = addAttributesToElement(fieldName, field.getName(), attributes);
        appendOpenTag(builder, fieldNameWithAttributes);
        builder.append(element);
        appendCloseTag(builder, getBaseElementName(fieldName));
      } else {
        String namespace = getNamespaceFromField(field);
        builder.append(xmlify(element, fieldName, namespace));
      }
    }
  }

  private void processSingleField(Field field, String fieldName, Object fieldValue,
                                  StringBuilder builder, Map<String, String> attributes) {
    if (isPrimitive(field.getType())) {
      String fieldNameWithAttributes = addAttributesToElement(fieldName, field.getName(), attributes);
      appendOpenTag(builder, fieldNameWithAttributes);
      builder.append(fieldValue);
      appendCloseTag(builder, getBaseElementName(fieldName));
    } else {
      String namespace = getNamespaceFromField(field);
      String elementName = containsNamespace(fieldName) ? getBaseElementName(fieldName) : fieldName;
      builder.append(xmlify(fieldValue, elementName, namespace));
    }
  }

  private String getBaseElementName(String fieldName) {
    return containsNamespace(fieldName) ? fieldName.split(":")[1] : fieldName;
  }

  private boolean containsNamespace(String fieldName) {
    return fieldName.contains(":");
  }

  private String getNamespaceFromField(Field field) {
    if (!field.isAnnotationPresent(Element.class)) {
      return null;
    }

    Element annotation = field.getAnnotation(Element.class);
    return annotation.namespace().isEmpty() ? null : annotation.namespace();
  }

  private String determineFieldName(Field field) {
    String fieldName = field.getName();

    if (field.isAnnotationPresent(Element.class)) {
      Element metadata = field.getAnnotation(Element.class);

      if (!metadata.name().isEmpty()) {
        fieldName = metadata.name();
      }
    }

    return fieldName;
  }

  private String determineRootName(String name, String namespace, Class<?> clazz) {
    String rootName = name != null ? name : clazz.getSimpleName().toLowerCase();

    if (clazz.isAnnotationPresent(RootElement.class) && namespace == null) {
      RootElement metadata = clazz.getDeclaredAnnotation(RootElement.class);

      if (!metadata.name().isEmpty()) {
        rootName = metadata.name();
      }

      if (!metadata.namespace().isEmpty()) {
        rootName = format(NAMESPACE_FORMAT, metadata.namespace(), rootName);
      }
    } else if (namespace != null) {
      rootName = format(NAMESPACE_FORMAT, namespace, rootName);
    }

    return rootName;
  }

  private String addAttributesToElement(String elementName, String key, Map<String, String> attributes) {
    if (!attributes.containsKey(key)) {
      return elementName;
    }

    return format(ATTRIBUTE_FORMAT, elementName, attributes.get(key));
  }

  private void addNamespaceDeclarations(Class<?> clazz, StringBuilder builder) {
    addExplicitNamespaces(clazz, builder);
    addDeclaredNamespaces(clazz, builder);
    addSingleNamespace(clazz, builder);
  }

  private void addExplicitNamespaces(Class<?> clazz, StringBuilder builder) {
    if (clazz.isAnnotationPresent(Namespaces.class)) {
      Namespaces namespacesAnnotation = clazz.getDeclaredAnnotation(Namespaces.class);

      for (Namespace namespace : namespacesAnnotation.namespaces()) {
        if (namespace.name().isEmpty() || namespace.value().isEmpty()) {
          continue;
        }

        builder.append(format(XMLNS_FORMAT, namespace.name(), namespace.value()));
      }
    }
  }

  private void addDeclaredNamespaces(Class<?> clazz, StringBuilder builder) {
    if (clazz.isAnnotationPresent(DeclaredNamespaces.class)) {
      DeclaredNamespaces aliases = clazz.getDeclaredAnnotation(DeclaredNamespaces.class);

      for (String alias : aliases.aliases()) {
        if (!namespaces.containsKey(alias)) {
          continue;
        }

        builder.append(format(XMLNS_FORMAT, alias, namespaces.get(alias)));
      }
    }
  }

  private void addSingleNamespace(Class<?> clazz, StringBuilder builder) {
    if (clazz.isAnnotationPresent(Namespace.class)) {
      Namespace namespace = clazz.getDeclaredAnnotation(Namespace.class);

      if (!namespace.name().isEmpty() && !namespace.value().isEmpty()) {
        builder.append(format(XMLNS_FORMAT, namespace.name(), namespace.value()));
      }
    }
  }

  private void appendOpenTag(StringBuilder builder, String name) {
    builder.append("<").append(name).append(">");
  }

  private void appendCloseTag(StringBuilder builder, String name) {
    builder.append("</").append(name).append(">");
  }

  public DefaultXmlizer registerNamespace(String name, String value) {
    if (isNullOrEmpty(name) || isNullOrEmpty(value)) {
      return this;
    }

    namespaces.put(name, value);
    return this;
  }

  public DefaultXmlizer registerNamespaces(Map<String, String> namespacesMap) {
    if (namespacesMap != null) {
      namespacesMap.forEach(this::registerNamespace);
    }
    return this;
  }

  private boolean isNullOrEmpty(String str) {
    return str == null || str.isEmpty();
  }

  private String xmlifyMap(Map<?, ?> map, String namespace) {
    StringBuilder sb = new StringBuilder();
    String localNamespace = namespace == null ? "" : namespace + ":";

    for (Map.Entry<?, ?> entry : map.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();

      if (value == null || !(key instanceof String)) {
        continue;
      }

      String keyStr = (String) key;
      String tagName = localNamespace + keyStr;

      appendOpenTag(sb, tagName);

      if (isPrimitive(value.getClass())) {
        sb.append(value);
      } else {
        sb.append(xmlify(value));
      }

      appendCloseTag(sb, tagName);
    }

    return sb.toString();
  }
}