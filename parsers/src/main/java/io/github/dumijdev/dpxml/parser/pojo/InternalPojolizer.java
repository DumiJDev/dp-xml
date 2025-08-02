package io.github.dumijdev.dpxml.parser.pojo;

import io.github.dumijdev.dpxml.annotations.*;
import io.github.dumijdev.dpxml.parser.api.AbstractPojolizer;
import io.github.dumijdev.dpxml.parser.exception.InternalErrorException;
import io.github.dumijdev.dpxml.parser.serializer.XmlDeserializer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Refactored InternalPojolizer with improved structure, scalability, and maintainability.
 * <p>
 * Key improvements:
 * - Thread-safe caching with ThreadLocal for factory instances
 * - Separated concerns into specialized handlers
 * - Immutable configurations and better error handling
 * - Reduced method complexity and improved readability
 */
public class InternalPojolizer extends AbstractPojolizer {

  // Thread-safe caches
  private static final Map<String, XPathExpression> XPATH_CACHE = new ConcurrentHashMap<>();
  private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();
  private static final Map<Class<?>, Constructor<?>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();
  private static final Map<Class<?>, List<Field>> INHERITANCE_FIELD_CACHE = new ConcurrentHashMap<>();
  private static final Map<String, Class<?>> TYPE_REGISTRY = new ConcurrentHashMap<>();

  // Thread-local factories for thread safety
  private static final ThreadLocal<XPathFactory> XPATH_FACTORY =
      ThreadLocal.withInitial(XPathFactory::newInstance);
  private static final ThreadLocal<XMLInputFactory> XML_INPUT_FACTORY =
      ThreadLocal.withInitial(() -> {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // Security configurations
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        return factory;
      });

  // Specialized handlers
  private final TypeConverterRegistry typeConverterRegistry = new TypeConverterRegistry();
  private final FieldProcessor fieldProcessor = new FieldProcessor();
  private final ConstructorResolver constructorResolver = new ConstructorResolver();
  private final XmlElementReader xmlElementReader = new XmlElementReader();
  private final InheritanceHandler inheritanceHandler = new InheritanceHandler();
  private final InterfaceProxyFactory interfaceProxyFactory = new InterfaceProxyFactory();

  /* Registers a type mapping for XML element names to concrete classes.
   * This is essential for interface and abstract class support.
   */
  public void registerType(String xmlElementName, Class<?> concreteClass) {
    TYPE_REGISTRY.put(xmlElementName, concreteClass);
  }

  /**
   * Registers multiple type mappings at once.
   */
  public void registerTypes(Map<String, Class<?>> typeMappings) {
    TYPE_REGISTRY.putAll(typeMappings);
  }

  @Override
  public <T> T pojoify(String xml, Class<T> clazz) {
    Objects.requireNonNull(xml, "XML string cannot be null");
    Objects.requireNonNull(clazz, "Class cannot be null");

    try (StringReader stringReader = new StringReader(xml)) {
      XMLStreamReader reader = XML_INPUT_FACTORY.get().createXMLStreamReader(stringReader);
      return parseObject(reader, clazz, XmlContext.root());
    } catch (XMLStreamException e) {
      throw new XmlProcessingException("Failed to parse XML", e);
    } catch (Exception e) {
      throw new InternalErrorException("Error processing XML: " + e.getMessage(), e);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T parseObject(XMLStreamReader reader, Class<T> clazz, XmlContext context)
      throws Exception {

    if (clazz == null) return null;

    // Handle interfaces and abstract classes
    Class<T> actualClass = inheritanceHandler.resolveActualClass(reader, clazz, context);

    // Handle custom deserializers
    Optional<T> customDeserialized = handleCustomDeserializer(reader, actualClass);
    if (customDeserialized.isPresent()) {
      return customDeserialized.get();
    }

    // Handle primitive types and strings
    if (typeConverterRegistry.isPrimitiveOrString(actualClass)) {
      String text = xmlElementReader.readElementText(reader);
      return (T) typeConverterRegistry.convertValue(text, actualClass);
    }

    // Create instance using cached constructor or proxy for interfaces
    T instance = createInstance(actualClass, reader, context);

    // Process fields including inherited fields
    inheritanceHandler.processInheritedFields(instance, reader, context, fieldProcessor);

    return instance;
  }

  @SuppressWarnings("unchecked")
  private <T> Optional<T> handleCustomDeserializer(XMLStreamReader reader, Class<T> clazz)
      throws Exception {

    XmlDeserialize annotation = clazz.getAnnotation(XmlDeserialize.class);
    if (annotation == null) return Optional.empty();

    String elementText = xmlElementReader.captureElementText(reader);
    XmlDeserializer<T> deserializer = (XmlDeserializer<T>)
        annotation.using().getDeclaredConstructor().newInstance();

    return Optional.of(deserializer.deserialize(elementText));
  }

  @SuppressWarnings("unchecked")
  private <T> T createInstance(Class<T> clazz, XMLStreamReader reader, XmlContext context)
      throws Exception {

    // Handle interfaces by creating dynamic proxies
    if (clazz.isInterface()) {
      return interfaceProxyFactory.createProxy(clazz, reader, context);
    }

    // Handle abstract classes
    if (Modifier.isAbstract(clazz.getModifiers())) {
      throw new XmlProcessingException(
          "Cannot instantiate abstract class: " + clazz.getName() +
              ". Register a concrete implementation using registerType()");
    }

    Constructor<?> constructor = CONSTRUCTOR_CACHE.computeIfAbsent(clazz,
        k -> constructorResolver.findBestConstructor(k, reader));

    if (constructor == null) {
      throw new XmlProcessingException(
          "No suitable constructor found for class: " + clazz.getName());
    }

    if (constructor.getParameterCount() == 0) {
      return (T) constructor.newInstance();
    }

    Object[] args = constructorResolver.createConstructorArgs(constructor, reader, context);
    return (T) constructor.newInstance(args);
  }

  // Field processing methods (simplified versions of original complex methods)
  private <T> void processField(T instance, Field field, XMLStreamReader reader,
                                XmlContext context) throws Exception {

    field.setAccessible(true);
    Class<?> fieldType = field.getType();

    // Handle custom deserializers
    if (field.isAnnotationPresent(XmlDeserialize.class)) {
      processCustomDeserializerField(instance, field, reader);
      return;
    }

    // Handle different field types
    if (fieldType.isArray()) {
      processArrayField(instance, field, reader, context);
    } else if (Collection.class.isAssignableFrom(fieldType)) {
      processCollectionField(instance, field, reader, context);
    } else if (Map.class.isAssignableFrom(fieldType)) {
      processMapField(instance, field, reader);
    } else if (typeConverterRegistry.isPrimitiveOrString(fieldType)) {
      String value = xmlElementReader.readElementText(reader);
      field.set(instance, typeConverterRegistry.convertValue(value, fieldType));
    } else {
      // Complex object
      Object fieldValue = parseObject(reader, fieldType, context.withPath(field.getName()));
      field.set(instance, fieldValue);
    }
  }

  private <T> void processCustomDeserializerField(T instance, Field field,
                                                  XMLStreamReader reader) throws Exception {

    String elementText = xmlElementReader.captureElementText(reader);
    XmlDeserializer<?> deserializer = field.getAnnotation(XmlDeserialize.class)
        .using().getDeclaredConstructor().newInstance();
    field.set(instance, deserializer.deserialize(elementText));
  }

  // Simplified versions of array, collection, and map processing methods
  private <T> void processArrayField(T instance, Field field, XMLStreamReader reader,
                                     XmlContext context) throws Exception {
    // Implementation similar to original but with better error handling
    // and separation of concerns
    Class<?> componentType = field.getType().getComponentType();
    List<Object> values = new ArrayList<>();

    if (typeConverterRegistry.isPrimitiveOrString(componentType)) {
      String elementText = xmlElementReader.captureElementText(reader);
      String[] items = elementText.trim().split("\\s+");
      for (String item : items) {
        values.add(typeConverterRegistry.convertValue(item, componentType));
      }
    } else {
      values.add(parseObject(reader, componentType, context));
    }

    Object array = Array.newInstance(componentType, values.size());
    for (int i = 0; i < values.size(); i++) {
      Array.set(array, i, values.get(i));
    }
    field.set(instance, array);
  }

  private <T> void processCollectionField(T instance, Field field, XMLStreamReader reader,
                                          XmlContext context) throws Exception {
    // Simplified implementation
    Type genericType = field.getGenericType();
    Class<?> componentType = extractGenericType(genericType, 0);

    Collection<Object> collection = createCollection(field.getType());

    var isPrimitiveOrString = typeConverterRegistry.isPrimitiveOrString(componentType);
    if (isPrimitiveOrString) {
      String elementText = xmlElementReader.captureElementText(reader);
      String[] items = elementText.trim().split("\\s+");
      for (String item : items) {
        collection.add(typeConverterRegistry.convertValue(item, componentType));
      }
    } else {
      collection.add(parseObject(reader, componentType, context));
    }

    field.set(instance, collection);
  }

  private <T> void processMapField(T instance, Field field, XMLStreamReader reader)
      throws Exception {
    // Simplified implementation
    Type genericType = field.getGenericType();
    Class<?> keyType = extractGenericType(genericType, 0);
    Class<?> valueType = extractGenericType(genericType, 1);

    Map<Object, Object> map = new LinkedHashMap<>();
    String elementText = xmlElementReader.captureElementText(reader);

    String[] pairs = elementText.trim().split("\\s+");
    for (String pair : pairs) {
      String[] kv = pair.split("=", 2);
      if (kv.length == 2) {
        Object key = typeConverterRegistry.convertValue(kv[0], keyType);
        Object value = typeConverterRegistry.convertValue(kv[1], valueType);
        map.put(key, value);
      }
    }

    field.set(instance, map);
  }

  // Utility methods
  private Class<?> extractGenericType(Type genericType, int index) {
    if (genericType instanceof ParameterizedType) {
      Type[] typeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
      if (typeArguments.length > index && typeArguments[index] instanceof Class) {
        return (Class<?>) typeArguments[index];
      }
    }
    return Object.class;
  }

  @SuppressWarnings("unchecked")
  private Collection<Object> createCollection(Class<?> collectionType) throws Exception {
    if (collectionType.isAssignableFrom(List.class)) {
      return new ArrayList<>();
    } else if (collectionType.isAssignableFrom(Set.class)) {
      return new LinkedHashSet<>();
    } else {
      return (Collection<Object>) collectionType.getDeclaredConstructor().newInstance();
    }
  }

  private boolean matchesXPath(String path, String xpathExpr) {
    try {
      XPathExpression expr = getXPathExpression(xpathExpr);
      return path.matches(convertXPathToRegex(xpathExpr));
    } catch (Exception e) {
      return false;
    }
  }

  private XPathExpression getXPathExpression(String xpath) throws XPathExpressionException {
    return XPATH_CACHE.computeIfAbsent(xpath, key -> {
      try {
        XPath xPath = XPATH_FACTORY.get().newXPath();
        return xPath.compile(key);
      } catch (XPathExpressionException e) {
        throw new RuntimeException("Failed to compile XPath: " + key, e);
      }
    });
  }

  private String convertXPathToRegex(String xpath) {
    return xpath.replace("/", "\\/")
        .replace("*", ".*")
        .replace("//", ".*");
  }

  /**
   * InvocationHandler for interface proxies
   */
  private static class InterfaceInvocationHandler implements InvocationHandler {
    private final Map<String, Object> propertyValues;
    private final Class<?> interfaceClass;

    InterfaceInvocationHandler(Map<String, Object> propertyValues, Class<?> interfaceClass) {
      this.propertyValues = propertyValues;
      this.interfaceClass = interfaceClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String methodName = method.getName();

      if (propertyValues.containsKey(methodName)) {
        return propertyValues.get(methodName);
      }

      // Handle getter methods
      if (methodName.startsWith("get") && args == null) {
        String propertyName = extractPropertyName(methodName, "get");
        return propertyValues.get(propertyName);
      } else if (methodName.startsWith("is") && args == null) {
        String propertyName = extractPropertyName(methodName, "is");
        return propertyValues.get(propertyName);
      }

      // Handle setter methods
      if (methodName.startsWith("set") && args != null && args.length == 1) {
        String propertyName = extractPropertyName(methodName, "set");
        propertyValues.put(propertyName, args[0]);
        return null;
      }

      // Handle Object methods
      if (methodName.equals("toString")) {
        return interfaceClass.getSimpleName() + propertyValues.toString();
      } else if (methodName.equals("hashCode")) {
        return propertyValues.hashCode();
      } else if (methodName.equals("equals") && args != null && args.length == 1) {
        return proxy == args[0];
      }

      // Default: return null or default value
      Class<?> returnType = method.getReturnType();
      if (returnType.isPrimitive()) {
        if (returnType == char.class) return '\0';
        if (returnType == boolean.class) return false;
        if (returnType == short.class) return (short) 0;
        if (returnType == byte.class) return (byte) 0;
        if (returnType == long.class) return 0L;
        if (returnType == int.class) return 0;
        if (returnType == double.class) return 0.0d;
        if (returnType == float.class) return 0.0f;
      }

      return null;
    }

    private String extractPropertyName(String methodName, String prefix) {
      String propertyName = methodName.substring(prefix.length());
      return propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
    }
  }

  /**
   * Immutable context for XML parsing state
   */
  private static class XmlContext {
    private final String currentPath;
    private final int depth;

    private XmlContext(String currentPath, int depth) {
      this.currentPath = currentPath;
      this.depth = depth;
    }

    static XmlContext root() {
      return new XmlContext("", 0);
    }

    XmlContext withPath(String path) {
      return new XmlContext(path, depth + 1);
    }

    String getCurrentPath() {
      return currentPath;
    }

    int getDepth() {
      return depth;
    }
  }

  /**
   * Registry for type converters with extensibility support
   */
  private static class TypeConverterRegistry {
    private final Map<Class<?>, XmlDeserializer<?>> converters = new HashMap<>();

    TypeConverterRegistry() {
      initializeDefaultConverters();
    }

    private void initializeDefaultConverters() {
      converters.put(String.class, s -> s);
      converters.put(Integer.class, Integer::parseInt);
      converters.put(int.class, Integer::parseInt);
      converters.put(Long.class, Long::parseLong);
      converters.put(long.class, Long::parseLong);
      converters.put(Double.class, Double::parseDouble);
      converters.put(double.class, Double::parseDouble);
      converters.put(Float.class, Float::parseFloat);
      converters.put(float.class, Float::parseFloat);
      converters.put(Boolean.class, Boolean::parseBoolean);
      converters.put(boolean.class, Boolean::parseBoolean);
      converters.put(Character.class, s -> s.isEmpty() ? null : s.charAt(0));
      converters.put(char.class, s -> s.isEmpty() ? '\0' : s.charAt(0));
      converters.put(Byte.class, Byte::parseByte);
      converters.put(byte.class, Byte::parseByte);
      converters.put(Short.class, Short::parseShort);
      converters.put(short.class, Short::parseShort);
    }

    Object convertValue(String value, Class<?> targetType) {
      if (value == null || value.isEmpty()) {
        return getDefaultValue(targetType);
      }

      var converter = converters.get(targetType);
      if (converter == null) {
        throw new IllegalArgumentException("No converter for type: " + targetType);
      }

      try {
        return converter.deserialize(value);
      } catch (Exception e) {
        throw new XmlProcessingException(
            String.format("Failed to convert '%s' to %s", value, targetType.getSimpleName()), e);
      }
    }

    boolean isPrimitiveOrString(Class<?> clazz) {
      return converters.containsKey(clazz);
    }

    private Object getDefaultValue(Class<?> type) {
      if (!type.isPrimitive()) return null;

      if (type == boolean.class) return false;
      if (type == char.class) return '\0';
      if (type == byte.class) return (byte) 0;
      if (type == short.class) return (short) 0;
      if (type == int.class) return 0;
      if (type == long.class) return 0L;
      if (type == float.class) return 0.0f;
      if (type == double.class) return 0.0d;

      return null;
    }

    // Extension point for custom converters
    void registerConverter(Class<?> type, XmlDeserializer<?> converter) {
      converters.put(type, converter);
    }
  }

  /**
   * Handles XML element reading operations
   */
  private static class XmlElementReader {

    String readElementText(XMLStreamReader reader) throws XMLStreamException {
      StringBuilder text = new StringBuilder();

      while (reader.hasNext()) {
        int event = reader.next();

        switch (event) {
          case XMLStreamConstants.CHARACTERS:
          case XMLStreamConstants.CDATA:
            text.append(reader.getText());
            break;
          case XMLStreamConstants.END_ELEMENT:
            return text.toString().trim();
        }
      }

      return text.toString().trim();
    }

    String captureElementText(XMLStreamReader reader) throws XMLStreamException {
      StringBuilder text = new StringBuilder();
      int depth = 1;

      while (reader.hasNext() && depth > 0) {
        int event = reader.next();

        switch (event) {
          case XMLStreamConstants.START_ELEMENT:
            depth++;
            appendStartElement(text, reader);
            break;
          case XMLStreamConstants.END_ELEMENT:
            depth--;
            if (depth > 0) {
              appendEndElement(text, reader);
            }
            break;
          case XMLStreamConstants.CHARACTERS:
          case XMLStreamConstants.CDATA:
            text.append(reader.getText());
            break;
        }
      }

      return text.toString();
    }

    private void appendStartElement(StringBuilder text, XMLStreamReader reader) {
      text.append("<").append(reader.getLocalName()).append(">");
    }

    private void appendEndElement(StringBuilder text, XMLStreamReader reader) {
      text.append("</").append(reader.getLocalName()).append(">");
    }
  }

  /**
   * Enhanced exception for XML processing errors
   */
  public static class XmlProcessingException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String xmlContext;

    public XmlProcessingException(String message) {
      this(message, null);
    }

    public XmlProcessingException(String message, Throwable cause) {
      this(message, cause, null);
    }

    public XmlProcessingException(String message, Throwable cause, String xmlContext) {
      super(message + (xmlContext != null ? " (Context: " + xmlContext + ")" : ""), cause);
      this.xmlContext = xmlContext;
    }

    public String getXmlContext() {
      return xmlContext;
    }
  }

  /**
   * Handles inheritance, abstract classes, and interface resolution
   */
  private class InheritanceHandler {

    @SuppressWarnings("unchecked")
    <T> Class<T> resolveActualClass(XMLStreamReader reader, Class<T> declaredClass,
                                    XmlContext context) throws XMLStreamException {

      // If it's a concrete class, use it as-is
      if (!declaredClass.isInterface() && !Modifier.isAbstract(declaredClass.getModifiers())) {
        return declaredClass;
      }

      // Try to resolve from XML element name
      if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
        String elementName = reader.getLocalName();
        Class<?> concreteClass = TYPE_REGISTRY.get(elementName);

        if (concreteClass != null && declaredClass.isAssignableFrom(concreteClass)) {
          return (Class<T>) concreteClass;
        }
      }

      // Try to resolve from type attribute (common XML pattern)
      if (reader.getEventType() == XMLStreamConstants.START_ELEMENT || reader.getEventType() == XMLStreamConstants.ATTRIBUTE) {
        String typeAttribute = reader.getAttributeValue(null, "type");
        if (typeAttribute != null) {
          Class<?> concreteClass = TYPE_REGISTRY.get(typeAttribute);
          if (concreteClass != null && declaredClass.isAssignableFrom(concreteClass)) {
            return (Class<T>) concreteClass;
          }
        }
      }

      // Try xsi:type attribute (XML Schema instance type)
      if (reader.getEventType() == XMLStreamConstants.START_ELEMENT || reader.getEventType() == XMLStreamConstants.ATTRIBUTE) {
        String xsiType = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance", "type");
        if (xsiType != null) {
          Class<?> concreteClass = TYPE_REGISTRY.get(xsiType);
          if (concreteClass != null && declaredClass.isAssignableFrom(concreteClass)) {
            return (Class<T>) concreteClass;
          }
        }
      }

      return declaredClass; // Return original if no concrete type found
    }

    <T> void processInheritedFields(T instance, XMLStreamReader reader, XmlContext context,
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
        return elementAnnotation.name();
      }
      return field.getName();
    }
  }

  /**
   * Factory for creating dynamic proxies for interfaces
   */
  private class InterfaceProxyFactory {

    @SuppressWarnings("unchecked")
    <T> T createProxy(Class<T> interfaceClass, XMLStreamReader reader, XmlContext context)
        throws Exception {

      if (!interfaceClass.isInterface()) {
        throw new IllegalArgumentException("Class must be an interface: " + interfaceClass);
      }

      // Parse XML into a map of property values
      Map<String, Object> propertyValues = parseInterfaceProperties(interfaceClass, reader, context);

      // Create dynamic proxy
      return (T) Proxy.newProxyInstance(
          interfaceClass.getClassLoader(),
          new Class<?>[]{interfaceClass},
          new InterfaceInvocationHandler(propertyValues, interfaceClass)
      );
    }

    private Map<String, Object> parseInterfaceProperties(Class<?> interfaceClass,
                                                         XMLStreamReader reader, XmlContext context) throws Exception {

      Map<String, Object> properties = new HashMap<>();

      // Get all methods that look like getters/setters
      Method[] methods = interfaceClass.getMethods();
      Set<String> propertyNames = extractPropertyNames(methods);

      // Parse XML elements that match property names
      while (reader.hasNext()) {
        int event = reader.next();

        if (event == XMLStreamConstants.START_ELEMENT) {
          String elementName = reader.getLocalName();

          if (propertyNames.contains(elementName)) {
            // Find the return type of the getter method
            Method getter = findGetterMethod(methods, elementName);
            if (getter != null) {
              Class<?> propertyType = getter.getReturnType();
              Object value = parsePropertyValue(reader, propertyType, context);
              properties.put(elementName, value);
            }
          }
        } else if (event == XMLStreamConstants.END_ELEMENT) {
          break;
        }
      }

      return properties;
    }

    private Set<String> extractPropertyNames(Method[] methods) {
      return Arrays.stream(methods)
          //.filter(method -> method.getName().startsWith("get") || method.getName().startsWith("is"))
          .filter(method -> method.getParameterCount() == 0)
          .filter(method -> !method.getReturnType().equals(void.class))
          .map(this::extractPropertyName)
          .collect(Collectors.toSet());
    }

    private String extractPropertyName(Method method) {
      String methodName = method.getName();
      String propertyName;

      if (methodName.startsWith("get")) {
        propertyName = methodName.substring(3);
      } else if (methodName.startsWith("is")) {
        propertyName = methodName.substring(2);
      } else {
        return methodName;
      }

      // Convert first character to lowercase
      return propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
    }

    private Method findGetterMethod(Method[] methods, String propertyName) {
      String getterName = "get" + capitalize(propertyName);
      String booleanGetterName = "is" + capitalize(propertyName);

      for (Method method : methods) {
        if ((method.getName().equals(getterName)
            || method.getName().equals(booleanGetterName)
            || method.getName().equals(propertyName))
            && method.getParameterCount() == 0) {
          return method;
        }
      }
      return null;
    }

    private String capitalize(String str) {
      if (str == null || str.isEmpty()) return str;
      return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private Object parsePropertyValue(XMLStreamReader reader, Class<?> propertyType,
                                      XmlContext context) throws Exception {

      if (typeConverterRegistry.isPrimitiveOrString(propertyType)) {
        String text = xmlElementReader.readElementText(reader);
        return typeConverterRegistry.convertValue(text, propertyType);
      } else {
        return parseObject(reader, propertyType, context);
      }
    }
  }

  /**
   * Handles field processing with caching and specialized handlers
   */
  private class FieldProcessor {

    <T> void processFields(T instance, XMLStreamReader reader, XmlContext context)
        throws Exception {

      Class<?> clazz = instance.getClass();
      Map<String, Field> fieldMap = getFieldMap(clazz);
      processFieldsWithMap(instance, reader, context, fieldMap);
    }

    <T> void processFieldsWithMap(T instance, XMLStreamReader reader, XmlContext context,
                                  Map<String, Field> fieldMap) throws Exception {

      FieldProcessingContext<T> processingContext = new FieldProcessingContext<>(
          instance, fieldMap, context);

      processXmlElements(reader, processingContext);
    }

    private Map<String, Field> getFieldMap(Class<?> clazz) {
      return FIELD_CACHE.computeIfAbsent(clazz, this::createFieldMap);
    }

    private Map<String, Field> createFieldMap(Class<?> clazz) {
      return Arrays.stream(clazz.getDeclaredFields())
          .filter(field -> !field.isAnnotationPresent(IgnoreElement.class))
          .peek(field -> field.setAccessible(true))
          .collect(Collectors.toMap(
              this::getFieldElementName,
              Function.identity(),
              (existing, replacement) -> existing
          ));
    }

    private String getFieldElementName(Field field) {
      Element elementAnnotation = field.getAnnotation(Element.class);
      if (elementAnnotation != null && !elementAnnotation.name().isEmpty()) {
        return elementAnnotation.name();
      }
      return field.getName();
    }

    private <T> void processXmlElements(XMLStreamReader reader,
                                        FieldProcessingContext<T> context) throws Exception {

      Stack<String> pathStack = new Stack<>();

      while (reader.hasNext()) {
        int event = reader.next();

        switch (event) {
          case XMLStreamConstants.START_ELEMENT:
            String elementName = reader.getLocalName();
            pathStack.push(elementName);

            Field field = context.fieldMap.get(elementName);
            if (field != null) {
              processField(context.instance, field, reader, context.xmlContext);
            }

            processFlexElements(context, pathStack, reader);
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

    private <T> void processFlexElements(FieldProcessingContext<T> context,
                                         Stack<String> pathStack, XMLStreamReader reader) throws Exception {

      String currentPath = String.join("/", pathStack);

      for (Field field : context.fieldMap.values()) {
        var flexAnnotation = field.getAnnotation(FlexElement.class);
        if (flexAnnotation != null && matchesXPath(currentPath, flexAnnotation.src())) {
          processField(context.instance, field, reader, context.xmlContext);
        }

        var xPathValueAnnotation = field.getAnnotation(XPathValue.class);
        if (xPathValueAnnotation != null && matchesXPath(currentPath, xPathValueAnnotation.value())) {
          processField(context.instance, field, reader, context.xmlContext);
        }
      }
    }

    private class FieldProcessingContext<T> {
      final T instance;
      final Map<String, Field> fieldMap;
      final XmlContext xmlContext;

      FieldProcessingContext(T instance, Map<String, Field> fieldMap, XmlContext xmlContext) {
        this.instance = instance;
        this.fieldMap = fieldMap;
        this.xmlContext = xmlContext;
      }
    }
  }

  /**
   * Handles constructor resolution and argument creation
   */
  private class ConstructorResolver {

    Constructor<?> findBestConstructor(Class<?> clazz, XMLStreamReader reader) {
      Constructor<?>[] constructors = clazz.getDeclaredConstructors();

      // Prefer no-arg constructor
      for (Constructor<?> constructor : constructors) {
        if (constructor.getParameterCount() == 0) {
          return constructor;
        }
      }

      // If no no-arg constructor, return first available
      return constructors.length > 0 ? constructors[0] : null;
    }

    Object[] createConstructorArgs(Constructor<?> constructor, XMLStreamReader reader,
                                   XmlContext context) throws Exception {

      Parameter[] parameters = constructor.getParameters();
      Object[] args = new Object[parameters.length];

      for (int i = 0; i < parameters.length; i++) {
        args[i] = createConstructorArg(parameters[i], reader, context);
      }

      return args;
    }

    private Object createConstructorArg(Parameter param, XMLStreamReader reader,
                                        XmlContext context) throws Exception {

      Class<?> type = param.getType();

      if (typeConverterRegistry.isPrimitiveOrString(type)) {
        String text = xmlElementReader.readElementText(reader);
        return typeConverterRegistry.convertValue(text, type);
      }

      // For complex types, delegate to main parsing method
      return parseObject(reader, type, context.withPath(param.getName()));
    }
  }
}