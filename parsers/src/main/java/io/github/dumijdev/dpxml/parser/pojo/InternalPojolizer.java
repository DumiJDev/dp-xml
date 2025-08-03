package io.github.dumijdev.dpxml.parser.pojo;

import io.github.dumijdev.dpxml.annotations.XmlDeserialize;
import io.github.dumijdev.dpxml.parser.api.AbstractPojolizer;
import io.github.dumijdev.dpxml.parser.exception.InternalErrorException;
import io.github.dumijdev.dpxml.parser.exception.XmlProcessingException;
import io.github.dumijdev.dpxml.parser.serializer.XmlDeserializer;
import io.github.dumijdev.dpxml.parser.utils.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
  private static final Map<Class<?>, Constructor<?>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();
  private static final Map<String, Class<?>> typeRegistry = new ConcurrentHashMap<>();


  private static final ThreadLocal<XMLInputFactory> XML_INPUT_FACTORY =
      ThreadLocal.withInitial(() -> {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // Security configurations
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        return factory;
      });

  // Specialized handlers
  private final FieldProcessor fieldProcessor = new FieldProcessor(this);
  private final ConstructorResolver constructorResolver = new ConstructorResolver(this);
  private final XmlElementReader xmlElementReader = new XmlElementReader();
  private final InheritanceHandler inheritanceHandler = new InheritanceHandler(typeRegistry);
  private final InterfaceProxyFactory interfaceProxyFactory = new InterfaceProxyFactory(this);

  @Override
  public <T> T pojoify(Reader reader, Class<T> clazz) {
    Objects.requireNonNull(reader, "XML string cannot be null");
    Objects.requireNonNull(clazz, "Class cannot be null");

    try {
      XMLStreamReader xmlStreamReader = XML_INPUT_FACTORY.get().createXMLStreamReader(reader);
      return parseObject(xmlStreamReader, clazz, XmlContext.root());
    } catch (XMLStreamException e) {
      throw new XmlProcessingException("Failed to parse XML", e);
    } catch (Exception e) {
      throw new InternalErrorException("Error processing XML: " + e.getMessage(), e);
    }
  }

  @SuppressWarnings("unchecked")
  public <T> T parseObject(XMLStreamReader reader, Class<T> clazz, XmlContext context)
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
    if (isPrimitiveOrString(actualClass)) {
      String text = xmlElementReader.readElementText(reader);
      return (T) convertValue(text, actualClass);
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
    XmlDeserializer<T> deserializer = (XmlDeserializer<T>) getDeserializer(annotation.using());

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

    constructor.setAccessible(true);

    if (constructor.getParameterCount() == 0) {
      return (T) constructor.newInstance();
    }

    Object[] args = constructorResolver.createConstructorArgs(constructor, reader, context);
    return (T) constructor.newInstance(args);
  }

}