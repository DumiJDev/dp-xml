package io.github.dumijdev.dpxml.parser.utils;

import io.github.dumijdev.dpxml.parser.pojo.InternalPojolizer;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory for creating dynamic proxies for interfaces
 */
public class InterfaceProxyFactory {
  private final InternalPojolizer pojolizer;
  private final XmlElementReader xmlElementReader = new XmlElementReader();

  public InterfaceProxyFactory(InternalPojolizer pojolizer) {
    this.pojolizer = pojolizer;
  }

  @SuppressWarnings("unchecked")
  public <T> T createProxy(Class<T> interfaceClass, XMLStreamReader reader, XmlContext context)
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

    if (pojolizer.isPrimitiveOrString(propertyType)) {
      String text = xmlElementReader.readElementText(reader);
      return pojolizer.convertValue(text, propertyType);
    } else {
      return pojolizer.parseObject(reader, propertyType, context);
    }
  }
}
