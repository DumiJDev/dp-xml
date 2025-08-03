package io.github.dumijdev.dpxml.parser.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InvocationHandler for interface proxies
 */
public class InterfaceInvocationHandler implements InvocationHandler {
  private final Map<String, Object> propertyValues;
  private final Class<?> interfaceClass;
  private static final Map<String, Class<?>> TYPE_REGISTRY = new ConcurrentHashMap<>();

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
