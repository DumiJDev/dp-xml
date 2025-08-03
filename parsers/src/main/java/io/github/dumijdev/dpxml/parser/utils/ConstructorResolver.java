package io.github.dumijdev.dpxml.parser.utils;

import io.github.dumijdev.dpxml.annotations.Element;
import io.github.dumijdev.dpxml.annotations.FlexElement;
import io.github.dumijdev.dpxml.annotations.XPathValue;
import io.github.dumijdev.dpxml.annotations.XmlDeserialize;
import io.github.dumijdev.dpxml.parser.pojo.InternalPojolizer;
import io.github.dumijdev.dpxml.parser.serializer.XmlDeserializer;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.lang.reflect.*;
import java.util.*;

/**
 * Handles constructor resolution and argument creation with advanced Element binding
 */
public class ConstructorResolver {
  private final InternalPojolizer pojolizer;
  private final XmlElementReader xmlElementReader = new XmlElementReader();
  private final XPathUtils xpathUtils = new XPathUtils();

  public ConstructorResolver(InternalPojolizer pojolizer) {
    this.pojolizer = pojolizer;
  }

  public Constructor<?> findBestConstructor(Class<?> clazz, XMLStreamReader reader) {
    Constructor<?>[] constructors = clazz.getDeclaredConstructors();

    // First, try to find constructor with most Element-annotated parameters
    Constructor<?> bestMatch = findConstructorWithMostElementBinding(constructors);
    if (bestMatch != null) {
      return bestMatch;
    }

    // Fallback to no-arg constructor
    for (Constructor<?> constructor : constructors) {
      if (constructor.getParameterCount() == 0) {
        return constructor;
      }
    }

    // If no no-arg constructor, return first available
    return constructors.length > 0 ? constructors[0] : null;
  }

  private Constructor<?> findConstructorWithMostElementBinding(Constructor<?>[] constructors) {
    Constructor<?> bestMatch = null;
    int maxElementAnnotations = -1;

    for (Constructor<?> constructor : constructors) {
      int elementCount = 0;
      for (Parameter param : constructor.getParameters()) {
        if (param.isAnnotationPresent(Element.class) ||
            param.isAnnotationPresent(FlexElement.class) ||
            param.isAnnotationPresent(XPathValue.class)) {
          elementCount++;
        }
      }

      if (elementCount > maxElementAnnotations) {
        maxElementAnnotations = elementCount;
        bestMatch = constructor;
      }
    }

    return maxElementAnnotations > 0 ? bestMatch : null;
  }

  public Object[] createConstructorArgs(Constructor<?> constructor, XMLStreamReader reader,
                                        XmlContext context) throws Exception {

    Parameter[] parameters = constructor.getParameters();
    Object[] args = new Object[parameters.length];

    // Create parameter map for easier processing
    Map<String, ParameterInfo> parameterMap = createParameterMap(parameters);

    ParameterProcessingContext processingContext = new ParameterProcessingContext(args, parameterMap, context);

    processXmlElementsForParameters(reader, processingContext);

    // Fill any remaining null values with defaults
    fillDefaultValues(args, parameters);

    return args;
  }

  private Map<String, ParameterInfo> createParameterMap(Parameter[] parameters) {
    Map<String, ParameterInfo> parameterMap = new HashMap<>();

    for (int i = 0; i < parameters.length; i++) {
      Parameter param = parameters[i];
      String elementName = getParameterElementName(param);
      parameterMap.put(elementName, new ParameterInfo(param, i));
    }

    return parameterMap;
  }

  private String getParameterElementName(Parameter param) {
    Element elementAnnotation = param.getAnnotation(Element.class);
    if (elementAnnotation != null && !elementAnnotation.name().isEmpty()) {
      return elementAnnotation.name();
    }
    return param.getName();
  }

  private void processXmlElementsForParameters(XMLStreamReader reader,
                                               ParameterProcessingContext context) throws Exception {

    Stack<String> pathStack = new Stack<>();

    while (reader.hasNext()) {
      int event = reader.next();

      switch (event) {
        case XMLStreamConstants.START_ELEMENT:
          String elementName = reader.getLocalName().trim();
          pathStack.push(elementName);

          ParameterInfo paramInfo = context.parameterMap.get(elementName);
          if (paramInfo != null && context.args[paramInfo.index] == null) {
            processParameter(context.args, paramInfo, reader, context.xmlContext);
          }

          processFlexParameters(context, pathStack, reader);
          break;

        case XMLStreamConstants.END_ELEMENT:
          if (!pathStack.isEmpty()) {
            pathStack.pop();
          }
          if (pathStack.isEmpty()) {
            return;
          }
          break;
      }
    }
  }

  private void processFlexParameters(ParameterProcessingContext context,
                                     Stack<String> pathStack, XMLStreamReader reader) throws Exception {

    String currentPath = String.join("/", pathStack);

    for (ParameterInfo paramInfo : context.parameterMap.values()) {
      if (context.args[paramInfo.index] != null) {
        continue; // Already processed
      }

      Parameter param = paramInfo.parameter;

      // Handle FlexElement annotation
      FlexElement flexAnnotation = param.getAnnotation(FlexElement.class);
      if (flexAnnotation != null && xpathUtils.matchesXPath(currentPath, flexAnnotation.src())) {
        processParameter(context.args, paramInfo, reader, context.xmlContext);
        continue;
      }

      // Handle XPathValue annotation
      XPathValue xPathValueAnnotation = param.getAnnotation(XPathValue.class);
      if (xPathValueAnnotation != null && xpathUtils.matchesXPath(currentPath, xPathValueAnnotation.value())) {
        processParameter(context.args, paramInfo, reader, context.xmlContext);
      }
    }
  }

  private void processParameter(Object[] args, ParameterInfo paramInfo, XMLStreamReader reader,
                                XmlContext context) throws Exception {

    Parameter param = paramInfo.parameter;
    Class<?> paramType = param.getType();

    // Handle custom deserializers
    if (param.isAnnotationPresent(XmlDeserialize.class)) {
      args[paramInfo.index] = processCustomDeserializerParameter(param, reader);
      return;
    }

    // Handle different parameter types
    if (paramType.isArray()) {
      args[paramInfo.index] = processArrayParameter(param, reader, context);
    } else if (Collection.class.isAssignableFrom(paramType)) {
      args[paramInfo.index] = processCollectionParameter(param, reader, context);
    } else if (Map.class.isAssignableFrom(paramType)) {
      args[paramInfo.index] = processMapParameter(param, reader);
    } else if (pojolizer.isPrimitiveOrString(paramType)) {
      String value = xmlElementReader.readElementText(reader);
      args[paramInfo.index] = pojolizer.convertValue(value, paramType);
    } else {
      // Complex object
      Object paramValue = pojolizer.parseObject(reader, paramType, context.withPath(param.getName()));
      args[paramInfo.index] = paramValue;
    }
  }

  private Object processCustomDeserializerParameter(Parameter param, XMLStreamReader reader) throws Exception {
    String elementText = xmlElementReader.captureElementText(reader);
    XmlDeserializer<?> deserializer = param.getAnnotation(XmlDeserialize.class)
        .using().getDeclaredConstructor().newInstance();
    return deserializer.deserialize(elementText);
  }

  private Object processArrayParameter(Parameter param, XMLStreamReader reader,
                                       XmlContext context) throws Exception {
    Class<?> componentType = param.getType().getComponentType();
    List<Object> tempList = new ArrayList<>();

    Object value = null;
    if (pojolizer.isPrimitiveOrString(componentType)) {
      String elementText = xmlElementReader.readElementText(reader);
      value = pojolizer.convertValue(elementText, componentType);
    } else {
      value = pojolizer.parseObject(reader, componentType, context);
    }

    if (value != null) {
      tempList.add(value);
    }

    // Convert list to array
    Object array = Array.newInstance(componentType, tempList.size());
    for (int i = 0; i < tempList.size(); i++) {
      Array.set(array, i, tempList.get(i));
    }

    return array;
  }

  private Object processCollectionParameter(Parameter param, XMLStreamReader reader,
                                            XmlContext context) throws Exception {
    Type genericType = param.getParameterizedType();
    Class<?> componentType = extractGenericType(genericType, 0);
    Class<?> collectionType = param.getType();

    Collection<Object> collection = createCollection(collectionType);

    if (pojolizer.isPrimitiveOrString(componentType)) {
      String elementText = xmlElementReader.readElementText(reader);
      collection.add(pojolizer.convertValue(elementText, componentType));
    } else {
      collection.add(pojolizer.parseObject(reader, componentType, context));
    }

    return collection;
  }

  private Object processMapParameter(Parameter param, XMLStreamReader reader) throws Exception {
    Type genericType = param.getParameterizedType();
    Class<?> keyType = extractGenericType(genericType, 0);
    Class<?> valueType = extractGenericType(genericType, 1);

    Map<Object, Object> map = new LinkedHashMap<>();
    String elementText = xmlElementReader.captureElementText(reader);

    // Parse key-value pairs (assuming format like "key1=value1 key2=value2")
    String[] pairs = elementText.trim().split("\\s+");
    for (String pair : pairs) {
      String[] kv = pair.split("=", 2);
      if (kv.length == 2) {
        Object key = pojolizer.convertValue(kv[0], keyType);
        Object value = pojolizer.convertValue(kv[1], valueType);
        map.put(key, value);
      }
    }

    return map;
  }

  @SuppressWarnings("unchecked")
  private Collection<Object> createCollection(Class<?> collectionType) throws Exception {
    if (collectionType.isAssignableFrom(List.class) || collectionType == List.class) {
      return new ArrayList<>();
    } else if (collectionType.isAssignableFrom(Set.class) || collectionType == Set.class) {
      return new LinkedHashSet<>();
    } else if (!collectionType.isInterface()) {
      return (Collection<Object>) collectionType.getDeclaredConstructor().newInstance();
    } else {
      return new ArrayList<>(); // Default fallback
    }
  }

  private void fillDefaultValues(Object[] args, Parameter[] parameters) throws Exception {
    for (int i = 0; i < args.length; i++) {
      if (args[i] == null) {
        Parameter param = parameters[i];

        // Try to get default value from Element annotation
        String defaultValue = getDefaultValueFromAnnotation(param);
        if (defaultValue != null) {
          args[i] = pojolizer.convertValue(defaultValue, param.getType());
        } else {
          // Use type default
          args[i] = getDefaultValue(param.getType());
        }
      }
    }
  }

  private String getDefaultValueFromAnnotation(Parameter param) {
    Element elementAnnotation = param.getAnnotation(Element.class);
    if (elementAnnotation != null) {
      // Assuming Element annotation has a defaultValue field
      // return elementAnnotation.defaultValue();
    }
    return null;
  }

  private Object getDefaultValue(Class<?> type) {
    if (type == boolean.class || type == Boolean.class) return false;
    if (type == byte.class || type == Byte.class) return (byte) 0;
    if (type == char.class || type == Character.class) return '\0';
    if (type == short.class || type == Short.class) return (short) 0;
    if (type == int.class || type == Integer.class) return 0;
    if (type == long.class || type == Long.class) return 0L;
    if (type == float.class || type == Float.class) return 0.0f;
    if (type == double.class || type == Double.class) return 0.0d;
    if (type == String.class) return "";
    return null;
  }

  private Class<?> extractGenericType(Type genericType, int index) {
    if (genericType instanceof ParameterizedType) {
      Type[] typeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
      if (typeArguments.length > index && typeArguments[index] instanceof Class) {
        return (Class<?>) typeArguments[index];
      }
    }
    return Object.class;
  }

  // Helper classes
  private static class ParameterInfo {
    final Parameter parameter;
    final int index;

    ParameterInfo(Parameter parameter, int index) {
      this.parameter = parameter;
      this.index = index;
    }

    @Override
    public String toString() {
      return "ParameterInfo{" +
          "parameter=" + parameter +
          ", index=" + index +
          '}';
    }
  }

  private static class ParameterProcessingContext {
    final Object[] args;
    final Map<String, ParameterInfo> parameterMap;
    final XmlContext xmlContext;

    ParameterProcessingContext(Object[] args, Map<String, ParameterInfo> parameterMap,
                               XmlContext xmlContext) {
      this.args = args;
      this.parameterMap = parameterMap;
      this.xmlContext = xmlContext;
    }

    @Override
    public String toString() {
      return "ParameterProcessingContext{" +
          "args=" + Arrays.toString(args) +
          ", parameterMap=" + parameterMap +
          ", xmlContext=" + xmlContext +
          '}';
    }
  }
}