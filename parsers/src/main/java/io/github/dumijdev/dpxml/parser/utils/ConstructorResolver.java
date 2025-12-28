package io.github.dumijdev.dpxml.parser.utils;

import io.github.dumijdev.dpxml.annotations.Element;
import io.github.dumijdev.dpxml.annotations.FlexElement;
import io.github.dumijdev.dpxml.annotations.XPathValue;
import io.github.dumijdev.dpxml.annotations.XmlDeserialize;
import io.github.dumijdev.dpxml.parser.pojo.InternalPojolizer;
import io.github.dumijdev.dpxml.parser.serializer.XmlDeserializer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

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
    var name = getParameterElementName(param);

    // Handle custom deserializers
    if (param.isAnnotationPresent(XmlDeserialize.class)) {
      args[paramInfo.index] = processCustomDeserializerParameter(param, reader);
      return;
    }

    var deserializer = pojolizer.getDeserializer(paramType);

    if (deserializer != null) {
      args[paramInfo.index] = deserializer.deserialize(xmlElementReader.captureElementText(reader));
      return;
    }

    // Handle different parameter types
    if (paramType.isArray()) {
      var value = processArrayParameter(param, reader, context);

      var array = args[paramInfo.index];

      if (array == null) {
        array = Array.newInstance(paramType.getComponentType(), 1);
        Array.set(array, 0, value);
        args[paramInfo.index] = array;
      } else {
        var length = Array.getLength(array);
        var newArray = Array.newInstance(paramType.getComponentType(), length + 1);
        System.arraycopy(array, 0, newArray, 0, length);
        Array.set(newArray, length, value);
        args[paramInfo.index] = newArray;
      }

    } else if (Collection.class.isAssignableFrom(paramType)) {
      var collection = args[paramInfo.index];
      var value = processCollectionParameter(param, reader, context);

      if (collection == null) {
        collection = createCollection(paramType);
      }

      if (value != null) {
        ((Collection<Object>) collection).add(value);
      }

      args[paramInfo.index] = collection;

    } else if (Map.class.isAssignableFrom(paramType)) {
      var map = args[paramInfo.index];

      if (map == null) {
        map = new LinkedHashMap<>();
        args[paramInfo.index] = map;
      }

      var value = processMapParameter(param, reader);

      if (value != null) {
        ((Map<Object, Object>) map).put(name, value);
      }

    } else if (pojolizer.isPrimitiveOrString(paramType)) {
      String value = xmlElementReader.readElementText(reader);
      args[paramInfo.index] = pojolizer.convertValue(value, paramType);
    } else {
      // Complex object
      Object paramValue = pojolizer.parseObject(reader, paramType, context.withPath(name));
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

    Object value = null;
    if (pojolizer.isPrimitiveOrString(componentType)) {
      String elementText = xmlElementReader.readElementText(reader);
      value = pojolizer.convertValue(elementText, componentType);
    } else {
      value = pojolizer.parseObject(reader, componentType, context);
    }

    return value;
  }

  private Object processCollectionParameter(Parameter param, XMLStreamReader reader,
                                            XmlContext context) throws Exception {
    Type genericType = param.getParameterizedType();
    Class<?> componentType = extractGenericType(genericType, 0);

    if (pojolizer.isPrimitiveOrString(componentType)) {
      String elementText = xmlElementReader.readElementText(reader);
      return pojolizer.convertValue(elementText, componentType);
    } else {
      return pojolizer.parseObject(reader, componentType, context);
    }
  }

  private Object processMapParameter(Parameter param, XMLStreamReader reader) throws Exception {
    Type genericType = param.getParameterizedType();
    Class<?> keyType = extractGenericType(genericType, 0);
    Class<?> valueType = extractGenericType(genericType, 1);

    String elementText = xmlElementReader.captureElementText(reader);

    return xmlToMap(elementText);
  }

  public Map<String, Object> xmlToMap(String xml) throws Exception {
    // Criação do DocumentBuilderFactory para fazer o parse do XML
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();

    // Criação de um InputStream a partir da String XML
    InputStream inputStream = new ByteArrayInputStream(xml.getBytes(UTF_8));

    // Parse do XML
    Document document = builder.parse(inputStream);

    // Mapeamento do XML para Map
    return parseElement(document.getDocumentElement());
  }

  private Map<String, Object> parseElement(org.w3c.dom.Element element) {
    Map<String, Object> map = new HashMap<>();

    // Obtendo todas as tags filhos do elemento
    NodeList childNodes = element.getChildNodes();

    for (int i = 0; i < childNodes.getLength(); i++) {
      Node node = childNodes.item(i);

      // Verifica se o nó é do tipo Element
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        var childElement = (org.w3c.dom.Element) node;
        var key = childElement.getTagName();
        var value = childElement.getTextContent().trim();

        // Adiciona ao Map
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
      return elementAnnotation.defaultValue();
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