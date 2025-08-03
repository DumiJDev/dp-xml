package io.github.dumijdev.dpxml.parser.xml;

import io.github.dumijdev.dpxml.annotations.*;
import io.github.dumijdev.dpxml.parser.api.AbstractXmlizer;
import io.github.dumijdev.dpxml.parser.exception.InternalErrorException;
import io.github.dumijdev.dpxml.parser.exception.UnXmlizableException;
import io.github.dumijdev.dpxml.parser.model.Node;
import io.github.dumijdev.dpxml.parser.serializer.XmlSerializer;
import io.github.dumijdev.dpxml.parser.utils.Attributes;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.dumijdev.dpxml.parser.utils.ParserUtils.*;

/**
 * Enhanced DefaultXmlizer with support for:
 * - @XmlSerialize for custom serialization
 * - @DynamicAttribute and @DynamicAttributes for dynamic attributes
 * - @DynamicAttributeGenerator for method-based attribute generation
 * - @StaticAttribute for static attributes
 * - Enhanced @Namespace support
 * - @DeclaredNamespaces for namespace aliases
 */
public class DefaultXmlizer extends AbstractXmlizer {

  // Thread-safe caches for XML processing components
  private static final ThreadLocal<DocumentBuilder> DOCUMENT_BUILDER =
      ThreadLocal.withInitial(DefaultXmlizer::createDocumentBuilder);

  private static final ThreadLocal<Transformer> TRANSFORMER =
      ThreadLocal.withInitial(DefaultXmlizer::createTransformer);

  // Namespace registry - thread-safe
  private final Map<String, String> namespaces = new ConcurrentHashMap<>();

  // Configuration options
  private boolean prettyPrint = false;
  private String encoding = "UTF-8";
  private boolean omitXmlDeclaration = true;

  /**
   * Factory methods for thread-local instances
   */
  private static DocumentBuilder createDocumentBuilder() {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setValidating(false);

      // Security configurations
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

      return factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("Failed to create DocumentBuilder", e);
    }
  }

  private static Transformer createTransformer() {
    try {
      TransformerFactory factory = TransformerFactory.newInstance();

      // Security configurations
      factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
      factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
      factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");

      return factory.newTransformer();
    } catch (Exception e) {
      throw new RuntimeException("Failed to create Transformer", e);
    }
  }

  /**
   * Static factory method for configuration
   */
  public static XmlConfiguration configure() {
    return new XmlConfiguration();
  }

  @Override
  public String xmlify(Object obj) {
    return xmlify(obj, null, null);
  }

  @Override
  public String xmlify(Node node) {
    if (node == null) {
      return "";
    }

    return node.asXml();
  }

  /**
   * Main xmlification method using DOM API
   */
  private String xmlify(Object obj, String name, String namespace) {
    if (obj == null) {
      return "";
    }

    try {
      Class<?> clazz = obj.getClass();

      if (!clazz.isAnnotationPresent(Xmlizable.class) && !isMap(clazz)) {
        throw new UnXmlizableException(clazz.getSimpleName());
      }

      Document document = DOCUMENT_BUILDER.get().newDocument();

      Element rootElement;
      if (isMap(clazz)) {
        rootElement = createMapElement(document, (Map<?, ?>) obj, namespace);
      } else {
        rootElement = createObjectElement(document, obj, name, namespace);
      }

      document.appendChild(rootElement);

      return documentToString(document);

    } catch (Exception e) {
      throw new InternalErrorException("Failed to xmlify object: " + e.getMessage(), e);
    }
  }

  /**
   * Creates XML element for regular objects
   */
  private Element createObjectElement(Document document, Object obj, String name, String namespace)
      throws IllegalAccessException, InvocationTargetException {

    Class<?> clazz = obj.getClass();
    String elementName = determineElementName(name, clazz);
    String fullElementName = applyNamespace(elementName, namespace, clazz);

    Element element = document.createElement(fullElementName);

    // Add namespace declarations
    addNamespaceDeclarations(element, clazz);

    // Add static attributes (class level)
    addStaticAttributes(element, clazz);

    // Add dynamic attributes (class level)
    addDynamicAttributes(element, clazz, obj);

    // Add dynamic attributes from generator methods
    addDynamicAttributesFromGenerators(element, clazz, obj);

    // Add regular attributes
    addAttributes(element, clazz, obj);

    // Process fields
    processObjectFields(document, element, obj, clazz);

    return element;
  }

  /**
   * Creates XML element for Map objects
   */
  private Element createMapElement(Document document, Map<?, ?> map, String namespace) throws InvocationTargetException, IllegalAccessException {
    Element containerElement = document.createElement("map");

    for (Map.Entry<?, ?> entry : map.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();

      if (value == null || !(key instanceof String)) {
        continue;
      }

      String keyStr = (String) key;
      String elementName = applyNamespace(keyStr, namespace, null);
      Element entryElement = document.createElement(elementName);

      if (isPrimitive(value.getClass())) {
        entryElement.setTextContent(String.valueOf(value));
      } else if (Map.class.isAssignableFrom(value.getClass())) {
        Element element = createMapElement(document, (Map<?, ?>) value, namespace);
        while (element.hasChildNodes()) {
          entryElement.appendChild(element.getFirstChild());
        }
        // Copy attributes
        copyAttributes(element, entryElement);
      } else {
        // For complex objects, create nested structure
        Element nestedElement = createObjectElement(document, value, keyStr, namespace);
        // Move children from nested element to entry element
        while (nestedElement.hasChildNodes()) {
          entryElement.appendChild(nestedElement.getFirstChild());
        }
        // Copy attributes
        copyAttributes(nestedElement, entryElement);
      }

      containerElement.appendChild(entryElement);
    }

    return containerElement;
  }

  /**
   * Processes all fields of an object
   */
  private void processObjectFields(Document document, Element parentElement, Object obj, Class<?> clazz)
      throws IllegalAccessException, InvocationTargetException {

    // Process fields from inheritance hierarchy
    List<Field> allFields = getAllFields(clazz);
    Map<String, String> attributes = Attributes.getAttributes(clazz, obj);

    for (Field field : allFields) {
      if (field.isAnnotationPresent(IgnoreElement.class)) {
        continue;
      }

      processField(document, parentElement, field, obj, attributes);
    }
  }

  /**
   * Processes a single field with custom serialization support
   */
  private void processField(Document document, Element parentElement, Field field, Object obj,
                            Map<String, String> attributes) throws IllegalAccessException, InvocationTargetException {

    field.setAccessible(true);
    Object fieldValue = field.get(obj);

    if (fieldValue == null) {
      return;
    }

    String fieldName = determineFieldName(field);
    String namespace = getNamespaceFromField(field);


    if (isCollection(field.getType())) {
      processCollectionField(document, parentElement, field, fieldName,
          (Collection<?>) fieldValue, attributes, namespace, obj);
    } else if (field.getType().isArray()) {
      processArrayField(document, parentElement, field, fieldName, fieldValue, attributes, namespace, obj);
    } else {
      processSingleField(document, parentElement, field, fieldName, fieldValue, attributes, namespace, obj);
    }
  }

  /**
   * Processes field with custom serialization
   */
  @SuppressWarnings("unchecked")
  private void processCustomSerializedField(Document document, Element parentElement, Field field,
                                            Object fieldValue, Map<String, String> attributes) throws IllegalAccessException {

    XmlSerialize annotation = field.getAnnotation(XmlSerialize.class);
    var serializerClass = annotation.using();

    try {
      XmlSerializer<Object> serializer = (XmlSerializer<Object>) getOrCreateSerializer(serializerClass);
      String serializedXml = serializer.serialize(fieldValue);

      // Parse the serialized XML and append to parent
      if (serializedXml != null && !serializedXml.trim().isEmpty()) {
        // For simplicity, we'll create a text node with the serialized content
        // In a more sophisticated implementation, you might parse the XML and append as elements
        String fieldName = determineFieldName(field);
        String namespace = getNamespaceFromField(field);
        String elementName = applyNamespace(fieldName, namespace, null);

        Element fieldElement = document.createElement(elementName);
        addFieldStaticAttributes(fieldElement, field);
        addFieldDynamicAttributes(fieldElement, field, fieldValue);
        addFieldAttributes(fieldElement, field.getName(), attributes);

        // For now, set as text content. Could be enhanced to parse and append XML structure
        fieldElement.setTextContent(serializedXml);
        parentElement.appendChild(fieldElement);
      }
    } catch (Exception e) {
      throw new IllegalAccessException("Failed to serialize field " + field.getName() + ": " + e.getMessage());
    }
  }

  /**
   * Gets or creates a serializer instance
   */
  private XmlSerializer<?> getOrCreateSerializer(Class<? extends XmlSerializer<?>> serializerClass) {
    return getSerializer(serializerClass);
  }

  /**
   * Processes collection fields
   */
  private void processCollectionField(Document document, Element parentElement, Field field,
                                      String fieldName, Collection<?> values, Map<String, String> attributes,
                                      String namespace, Object parentObj) throws InvocationTargetException, IllegalAccessException {

    for (Object element : values) {
      if (element == null) {
        continue;
      }

      String elementName = applyNamespace(fieldName, namespace, null);
      Element xmlElement = document.createElement(elementName);

      addFieldStaticAttributes(xmlElement, field);
      addFieldDynamicAttributes(xmlElement, field, parentObj);
      addFieldAttributes(xmlElement, field.getName(), attributes);

      // Check for custom serialization
      if (field.isAnnotationPresent(XmlSerialize.class)) {
        processCustomSerializedField(document, parentElement, field, element, attributes);
        return;
      }

      if (isPrimitive(element.getClass())) {
        xmlElement.setTextContent(String.valueOf(element));
      } else {
        Element nestedElement = createObjectElement(document, element, fieldName, namespace);
        // Move children from nested to current element
        while (nestedElement.hasChildNodes()) {
          xmlElement.appendChild(nestedElement.getFirstChild());
        }
        copyAttributes(nestedElement, xmlElement);
      }

      parentElement.appendChild(xmlElement);
    }
  }

  /**
   * Processes array fields
   */
  private void processArrayField(Document document, Element parentElement, Field field,
                                 String fieldName, Object arrayValue, Map<String, String> attributes,
                                 String namespace, Object parentObj) throws InvocationTargetException, IllegalAccessException {

    int length = Array.getLength(arrayValue);

    for (int i = 0; i < length; i++) {
      Object element = Array.get(arrayValue, i);
      if (element == null) {
        continue;
      }

      String elementName = applyNamespace(fieldName, namespace, null);
      Element xmlElement = document.createElement(elementName);

      addFieldStaticAttributes(xmlElement, field);
      addFieldDynamicAttributes(xmlElement, field, parentObj);
      addFieldAttributes(xmlElement, field.getName(), attributes);

      // Check for custom serialization
      if (field.isAnnotationPresent(XmlSerialize.class)) {
        processCustomSerializedField(document, parentElement, field, element, attributes);
        return;
      }

      if (isPrimitive(element.getClass())) {
        xmlElement.setTextContent(String.valueOf(element));
      } else {
        Element nestedElement = createObjectElement(document, element, fieldName, namespace);
        while (nestedElement.hasChildNodes()) {
          xmlElement.appendChild(nestedElement.getFirstChild());
        }
        copyAttributes(nestedElement, xmlElement);
      }

      parentElement.appendChild(xmlElement);
    }
  }

  /**
   * Processes single (non-collection) fields
   */
  private void processSingleField(Document document, Element parentElement, Field field,
                                  String fieldName, Object fieldValue, Map<String, String> attributes,
                                  String namespace, Object parentObj) throws InvocationTargetException, IllegalAccessException {

    try {
      document.createElement(fieldName);
    } catch (DOMException e) {
      if (e.code == DOMException.INVALID_CHARACTER_ERR) {
        return;
      }
    }

    String elementName = applyNamespace(fieldName, namespace, null);
    Element xmlElement = document.createElement(elementName);

    addFieldStaticAttributes(xmlElement, field);
    addFieldDynamicAttributes(xmlElement, field, parentElement);
    addFieldAttributes(xmlElement, field.getName(), attributes);

    // Check for custom serialization
    if (field.isAnnotationPresent(XmlSerialize.class)) {
      processCustomSerializedField(document, parentElement, field, fieldValue, attributes);
      return;
    }

    if (isPrimitive(field.getType())) {
      xmlElement.setTextContent(String.valueOf(fieldValue));
    } else {
      Element nestedElement = createObjectElement(document, fieldValue, fieldName, namespace);
      while (nestedElement.hasChildNodes()) {
        xmlElement.appendChild(nestedElement.getFirstChild());
      }
      copyAttributes(nestedElement, xmlElement);
    }

    parentElement.appendChild(xmlElement);
  }

  /**
   * Adds static attributes to element (class level)
   */
  private void addStaticAttributes(Element element, Class<?> clazz) {
    // Single static attribute
    if (clazz.isAnnotationPresent(StaticAttribute.class)) {
      StaticAttribute attr = clazz.getAnnotation(StaticAttribute.class);
      element.setAttribute(attr.name(), attr.value());
    }

    // Multiple static attributes (if such annotation exists)
    // This would require a @StaticAttributes annotation similar to @DynamicAttributes
  }

  /**
   * Adds static attributes to field element
   */
  private void addFieldStaticAttributes(Element element, Field field) {
    if (field.isAnnotationPresent(StaticAttribute.class)) {
      StaticAttribute attr = field.getAnnotation(StaticAttribute.class);
      element.setAttribute(attr.name(), attr.value());
    }
  }

  /**
   * Adds dynamic attributes to element (class level)
   */
  private void addDynamicAttributes(Element element, Class<?> clazz, Object obj) {
    // Multiple dynamic attributes
    if (clazz.isAnnotationPresent(DynamicAttributes.class)) {
      DynamicAttributes attrs = clazz.getAnnotation(DynamicAttributes.class);
      for (DynamicAttribute attr : attrs.attributes()) {
        addSingleDynamicAttribute(element, attr, obj);
      }
    }

    // Single dynamic attribute
    if (clazz.isAnnotationPresent(DynamicAttribute.class)) {
      DynamicAttribute attr = clazz.getAnnotation(DynamicAttribute.class);
      addSingleDynamicAttribute(element, attr, obj);
    }
  }

  /**
   * Adds dynamic attributes to field element
   */
  private void addFieldDynamicAttributes(Element element, Field field, Object fieldValue) {
    // Multiple dynamic attributes
    if (field.isAnnotationPresent(DynamicAttributes.class)) {
      DynamicAttributes attrs = field.getAnnotation(DynamicAttributes.class);
      for (DynamicAttribute attr : attrs.attributes()) {
        addSingleDynamicAttribute(element, attr, fieldValue);
      }
    }

    // Single dynamic attribute
    if (field.isAnnotationPresent(DynamicAttribute.class)) {
      DynamicAttribute attr = field.getAnnotation(DynamicAttribute.class);
      addSingleDynamicAttribute(element, attr, fieldValue);
    }
  }

  /**
   * Adds a single dynamic attribute by calling the specified method
   */
  private void addSingleDynamicAttribute(Element element, DynamicAttribute attr, Object obj) {
    try {
      Class<?> clazz = obj.getClass();
      Method method = findMethod(clazz, attr.method());
      if (method != null) {
        method.setAccessible(true);
        Object result = method.invoke(obj);
        if (result != null) {
          element.setAttribute(attr.name(), result.toString());
        }
      }
    } catch (Exception e) {
      // Log warning but don't fail the entire serialization
      System.err.println("Warning: Failed to execute dynamic attribute method " +
          attr.method() + ": " + e.getMessage());
    }
  }

  /**
   * Adds dynamic attributes from generator methods
   */
  private void addDynamicAttributesFromGenerators(Element element, Class<?> clazz, Object obj) {
    Method[] methods = clazz.getDeclaredMethods();
    for (Method method : methods) {
      if (method.isAnnotationPresent(DynamicAttributeGenerator.class)) {
        DynamicAttributeGenerator generator = method.getAnnotation(DynamicAttributeGenerator.class);
        try {
          method.setAccessible(true);

          // Prepare arguments based on specified fields
          Object[] args = prepareGeneratorArguments(generator.fields(), obj);
          Object result = method.invoke(obj, args);

          if (result != null) {
            element.setAttribute(generator.name(), result.toString());
          }
        } catch (Exception e) {
          System.err.println("Warning: Failed to execute dynamic attribute generator " +
              method.getName() + ": " + e.getMessage());
        }
      }
    }
  }

  /**
   * Prepares arguments for dynamic attribute generator methods
   */
  private Object[] prepareGeneratorArguments(String[] fieldNames, Object obj) throws IllegalAccessException {
    Object[] args = new Object[fieldNames.length];
    Class<?> clazz = obj.getClass();

    for (int i = 0; i < fieldNames.length; i++) {
      try {
        Field field = findField(clazz, fieldNames[i]);
        if (field != null) {
          field.setAccessible(true);
          args[i] = field.get(obj);
        } else {
          args[i] = null;
        }
      } catch (Exception e) {
        args[i] = null;
      }
    }

    return args;
  }

  /**
   * Finds a method by name in class hierarchy
   */
  private Method findMethod(Class<?> clazz, String methodName) {
    Class<?> currentClass = clazz;
    while (currentClass != null && currentClass != Object.class) {
      try {
        return currentClass.getDeclaredMethod(methodName);
      } catch (NoSuchMethodException e) {
        // Try with different parameter types if needed
        Method[] methods = currentClass.getDeclaredMethods();
        for (Method method : methods) {
          if (method.getName().equals(methodName)) {
            return method;
          }
        }
      }
      currentClass = currentClass.getSuperclass();
    }
    return null;
  }

  /**
   * Finds a field by name in class hierarchy
   */
  private Field findField(Class<?> clazz, String fieldName) {
    Class<?> currentClass = clazz;
    while (currentClass != null && currentClass != Object.class) {
      try {
        return currentClass.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        // Continue to parent class
      }
      currentClass = currentClass.getSuperclass();
    }
    return null;
  }

  /**
   * Determines the element name for a class
   */
  private String determineElementName(String name, Class<?> clazz) {
    if (name != null && !name.isEmpty()) {
      return name;
    }

    if (clazz.isAnnotationPresent(RootElement.class)) {
      RootElement metadata = clazz.getDeclaredAnnotation(RootElement.class);
      if (!metadata.name().isEmpty()) {
        return metadata.name();
      }
    }

    return clazz.getSimpleName().toLowerCase();
  }

  /**
   * Determines the field name considering annotations
   */
  private String determineFieldName(Field field) {
    if (field.isAnnotationPresent(io.github.dumijdev.dpxml.annotations.Element.class)) {
      var metadata = field.getAnnotation(io.github.dumijdev.dpxml.annotations.Element.class);
      if (!metadata.name().isEmpty()) {
        return metadata.name();
      }
    }
    return field.getName();
  }

  /**
   * Applies namespace to element name with enhanced logic
   */
  private String applyNamespace(String elementName, String namespace, Class<?> clazz) {
    // Check for explicit namespace from parameter
    if (namespace != null && !namespace.isEmpty()) {
      return namespace + ":" + elementName;
    }

    // Check for class-level namespace
    if (clazz != null && clazz.isAnnotationPresent(Namespace.class)) {
      Namespace namespaceAnnotation = clazz.getAnnotation(Namespace.class);
      String namespacePrefix = namespaceAnnotation.name();
      if (!namespacePrefix.isEmpty()) {
        return namespacePrefix + ":" + elementName;
      }
    }

    // Check for root element namespace
    if (clazz != null && clazz.isAnnotationPresent(RootElement.class)) {
      RootElement metadata = clazz.getDeclaredAnnotation(RootElement.class);
      if (!metadata.namespace().isEmpty()) {
        return metadata.namespace() + ":" + elementName;
      }
    }

    return elementName;
  }

  /**
   * Gets namespace from field annotation
   */
  private String getNamespaceFromField(Field field) {
    // Check field-level namespace annotation
    if (field.isAnnotationPresent(Namespace.class)) {
      Namespace namespaceAnnotation = field.getAnnotation(Namespace.class);
      return namespaceAnnotation.name().isEmpty() ? null : namespaceAnnotation.name();
    }

    // Check element annotation namespace
    if (field.isAnnotationPresent(io.github.dumijdev.dpxml.annotations.Element.class)) {
      var annotation = field.getAnnotation(io.github.dumijdev.dpxml.annotations.Element.class);
      return annotation.namespace().isEmpty() ? null : annotation.namespace();
    }
    return null;
  }

  /**
   * Adds namespace declarations to element with enhanced support
   */
  private void addNamespaceDeclarations(Element element, Class<?> clazz) {
    // Add explicit namespaces
    if (clazz.isAnnotationPresent(Namespaces.class)) {
      Namespaces namespacesAnnotation = clazz.getDeclaredAnnotation(Namespaces.class);
      for (Namespace namespace : namespacesAnnotation.namespaces()) {
        addNamespaceDeclaration(element, namespace);
      }
    }

    // Add single namespace
    if (clazz.isAnnotationPresent(Namespace.class)) {
      Namespace namespace = clazz.getDeclaredAnnotation(Namespace.class);
      addNamespaceDeclaration(element, namespace);
    }

    // Add declared namespaces (aliases)
    if (clazz.isAnnotationPresent(DeclaredNamespaces.class)) {
      DeclaredNamespaces aliases = clazz.getDeclaredAnnotation(DeclaredNamespaces.class);
      for (String alias : aliases.aliases()) {
        if (namespaces.containsKey(alias)) {
          element.setAttributeNS("http://www.w3.org/2000/xmlns/",
              "xmlns:" + alias, namespaces.get(alias));
        }
      }
    }
  }

  /**
   * Adds a single namespace declaration
   */
  private void addNamespaceDeclaration(Element element, Namespace namespace) {
    if (!namespace.value().isEmpty()) {
      String prefix = namespace.name().isEmpty() ? "xmlns" : "xmlns:" + namespace.name();
      element.setAttributeNS("http://www.w3.org/2000/xmlns/", prefix, namespace.value());
    }
  }

  /**
   * Adds attributes to element
   */
  private void addAttributes(Element element, Class<?> clazz, Object obj)
      throws IllegalAccessException, InvocationTargetException {

    Map<String, String> attributes = Attributes.getAttributes(clazz, obj);
    addFieldAttributes(element, clazz.getName(), attributes);
  }

  /**
   * Adds field-specific attributes
   */
  private void addFieldAttributes(Element element, String key, Map<String, String> attributes) {
    if (attributes.containsKey(key)) {
      String attributeString = attributes.get(key);
      // Parse attribute string and add to element
      // Assuming format: "attr1='value1' attr2='value2'"
      parseAndAddAttributes(element, attributeString);
    }
  }

  /**
   * Parses attribute string and adds to element
   */
  private void parseAndAddAttributes(Element element, String attributeString) {
    if (attributeString == null || attributeString.trim().isEmpty()) {
      return;
    }

    // Simple attribute parsing (could be enhanced for complex scenarios)
    String[] attributePairs = attributeString.split("=");

    if (attributePairs.length != 2) {
      return;
    }

    var key = attributePairs[0].trim();
    var value = attributePairs[1].trim();
    element.setAttribute(key, value);

  }

  /**
   * Copies attributes from source to target element
   */
  private void copyAttributes(Element source, Element target) {
    org.w3c.dom.NamedNodeMap attributes = source.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      org.w3c.dom.Node attr = attributes.item(i);
      target.setAttribute(attr.getNodeName(), attr.getNodeValue());
    }
  }

  /**
   * Gets all fields including inherited ones
   */
  private List<Field> getAllFields(Class<?> clazz) {
    List<Field> allFields = new ArrayList<>();
    Class<?> currentClass = clazz;

    while (currentClass != null && currentClass != Object.class) {
      Field[] declaredFields = currentClass.getDeclaredFields();

      for (Field field : declaredFields) {
        // Add field if not already present (child class takes precedence)
        if (allFields.stream().noneMatch(f -> f.getName().equals(field.getName()))) {
          allFields.add(field);
        }
      }

      currentClass = currentClass.getSuperclass();
    }

    return allFields;
  }

  /**
   * Converts DOM Document to String
   */
  private String documentToString(Document document) throws TransformerException {
    Transformer transformer = TRANSFORMER.get();

    // Configure transformer based on settings
    transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration ? "yes" : "no");
    transformer.setOutputProperty(OutputKeys.INDENT, prettyPrint ? "yes" : "no");

    if (prettyPrint) {
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    }

    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(document), new StreamResult(writer));

    return writer.toString();
  }

  /**
   * Configuration methods
   */
  public DefaultXmlizer withPrettyPrint(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
    return this;
  }

  public DefaultXmlizer withEncoding(String encoding) {
    this.encoding = encoding;
    return this;
  }

  public DefaultXmlizer withXmlDeclaration(boolean includeDeclaration) {
    this.omitXmlDeclaration = !includeDeclaration;
    return this;
  }

  /**
   * Namespace registration methods
   */
  public DefaultXmlizer registerNamespace(String name, String value) {
    if (name != null && !name.isEmpty() && value != null && !value.isEmpty()) {
      namespaces.put(name, value);
    }
    return this;
  }

  public DefaultXmlizer registerNamespaces(Map<String, String> namespacesMap) {
    if (namespacesMap != null) {
      namespacesMap.forEach(this::registerNamespace);
    }
    return this;
  }

  /**
   * Utility class for enhanced XML processing
   */
  public static class XmlConfiguration {
    private boolean prettyPrint = false;
    private String encoding = "UTF-8";
    private boolean includeXmlDeclaration = false;
    private Map<String, String> namespaces = new HashMap<>();

    public XmlConfiguration prettyPrint() {
      this.prettyPrint = true;
      return this;
    }

    public XmlConfiguration encoding(String encoding) {
      this.encoding = encoding;
      return this;
    }

    public XmlConfiguration includeXmlDeclaration() {
      this.includeXmlDeclaration = true;
      return this;
    }

    public XmlConfiguration namespace(String prefix, String uri) {
      namespaces.put(prefix, uri);
      return this;
    }

    public DefaultXmlizer build() {
      return new DefaultXmlizer()
          .withPrettyPrint(prettyPrint)
          .withEncoding(encoding)
          .withXmlDeclaration(includeXmlDeclaration)
          .registerNamespaces(namespaces);
    }
  }
}

/**
 * Example usage and annotations:
 * <p>
 * // Custom serializer example
 * public class DateSerializer implements DefaultXmlizer.XmlSerializer<Date> {
 *
 * @Override public String serialize(Date date) {
 * return new SimpleDateFormat("yyyy-MM-dd").format(date);
 * }
 * }
 * <p>
 * // Usage example
 * @Xmlizable
 * @Namespace(name = "app", value = "http://example.com/app")
 * @DeclaredNamespaces(aliases = {"common", "util"})
 * @StaticAttribute(name = "version", value = "1.0")
 * @DynamicAttribute(name = "timestamp", method = "getCurrentTimestamp")
 * public class Person {
 * @Element(name = "full-name")
 * @Namespace(name = "person", value = "http://example.com/person")
 * private String name;
 * @XmlSerialize(using = DateSerializer.class)
 * private Date birthDate;
 * @StaticAttribute(name = "type", value = "contact")
 * @DynamicAttribute(name = "hash", method = "calculateHash")
 * private List<String> emails;
 * @DynamicAttributeGenerator(name = "fullInfo", fields = {"name", "birthDate"})
 * public String generateFullInfo(String name, Date birthDate) {
 * return name + "-" + (birthDate != null ? birthDate.getTime() : "unknown");
 * }
 * <p>
 * public String getCurrentTimestamp() {
 * return String.valueOf(System.currentTimeMillis());
 * }
 * <p>
 * public String calculateHash() {
 * return String.valueOf(emails.hashCode());
 * }
 * }
 */