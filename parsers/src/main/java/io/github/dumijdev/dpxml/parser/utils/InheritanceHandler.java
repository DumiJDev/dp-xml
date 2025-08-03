package io.github.dumijdev.dpxml.parser.utils;

import io.github.dumijdev.dpxml.annotations.Element;
import io.github.dumijdev.dpxml.annotations.IgnoreElement;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles inheritance, abstract classes, and interface resolution
 */
public class InheritanceHandler {
  private static final Map<Class<?>, List<Field>> INHERITANCE_FIELD_CACHE = new ConcurrentHashMap<>();
  private final Map<String, Class<?>> typeRegistry;

  public InheritanceHandler(Map<String, Class<?>> typeRegistry) {
    this.typeRegistry = typeRegistry;
  }

  @SuppressWarnings("unchecked")
  public <T> Class<T> resolveActualClass(XMLStreamReader reader, Class<T> declaredClass,
                                         XmlContext context) throws XMLStreamException {

    // If it's a concrete class, use it as-is
    if (!declaredClass.isInterface() && !Modifier.isAbstract(declaredClass.getModifiers())) {
      return declaredClass;
    }

    // Try to resolve from XML element name
    if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
      String elementName = reader.getLocalName();
      Class<?> concreteClass = typeRegistry.get(elementName);

      if (concreteClass != null && declaredClass.isAssignableFrom(concreteClass)) {
        return (Class<T>) concreteClass;
      }
    }

    // Try to resolve from type attribute (common XML pattern)
    if (reader.getEventType() == XMLStreamConstants.START_ELEMENT || reader.getEventType() == XMLStreamConstants.ATTRIBUTE) {
      String typeAttribute = reader.getAttributeValue(null, "type");
      if (typeAttribute != null) {
        Class<?> concreteClass = typeRegistry.get(typeAttribute);
        if (concreteClass != null && declaredClass.isAssignableFrom(concreteClass)) {
          return (Class<T>) concreteClass;
        }
      }
    }

    // Try xsi:type attribute (XML Schema instance type)
    if (reader.getEventType() == XMLStreamConstants.START_ELEMENT || reader.getEventType() == XMLStreamConstants.ATTRIBUTE) {
      String xsiType = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance", "type");
      if (xsiType != null) {
        Class<?> concreteClass = typeRegistry.get(xsiType);
        if (concreteClass != null && declaredClass.isAssignableFrom(concreteClass)) {
          return (Class<T>) concreteClass;
        }
      }
    }

    return declaredClass; // Return original if no concrete type found
  }

  public <T> void processInheritedFields(T instance, XMLStreamReader reader, XmlContext context,
                                         FieldProcessor fieldProcessor) throws Exception {

    Class<?> currentClass = instance.getClass();

    // Collect all fields from inheritance hierarchy
    List<Field> allFields = getAllInheritedFields(currentClass);

    // Create enhanced field processor context with inherited fields
    Map<String, Field> fieldMap = allFields.stream()
        .filter(field -> !field.isAnnotationPresent(IgnoreElement.class))
        .peek(field -> field.setAccessible(true))
        .collect(Collectors.toMap(
            this::getFieldElementName,
            Function.identity(),
            (existing, replacement) -> existing // Keep first occurrence
        ));

    // Process fields using enhanced context
    fieldProcessor.processFieldsWithMap(instance, reader, context, fieldMap);
  }

  private List<Field> getAllInheritedFields(Class<?> clazz) {
    return INHERITANCE_FIELD_CACHE.computeIfAbsent(clazz, this::collectInheritedFields);
  }

  private List<Field> collectInheritedFields(Class<?> clazz) {
    List<Field> allFields = new ArrayList<>();
    Class<?> currentClass = clazz;

    // Walk up the inheritance hierarchy
    while (currentClass != null && currentClass != Object.class) {
      Field[] declaredFields = currentClass.getDeclaredFields();

      // Add fields that aren't already present (child class fields take precedence)
      for (Field field : declaredFields) {
        if (allFields.stream().noneMatch(f -> f.getName().equals(field.getName()))) {
          allFields.add(field);
        }
      }

      currentClass = currentClass.getSuperclass();
    }

    // Also collect fields from interfaces
    collectInterfaceFields(clazz, allFields);

    return allFields;
  }

  private void collectInterfaceFields(Class<?> clazz, List<Field> allFields) {

    if (clazz == null) {
      return;
    }

    for (Class<?> interfaceClass : clazz.getInterfaces()) {
      Field[] interfaceFields = interfaceClass.getDeclaredFields();

      for (Field field : interfaceFields) {
        // Only add static final fields from interfaces (constants)
        if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
          if (allFields.stream().noneMatch(f -> f.getName().equals(field.getName()))) {
            allFields.add(field);
          }
        }
      }

      // Recursively collect from parent interfaces
      collectInterfaceFields(interfaceClass, allFields);
    }
  }

  private String getFieldElementName(Field field) {
    Element elementAnnotation = field.getAnnotation(Element.class);
    if (elementAnnotation != null && !elementAnnotation.name().isEmpty()) {
      return elementAnnotation.name().trim();
    }
    return field.getName().trim();
  }
}
