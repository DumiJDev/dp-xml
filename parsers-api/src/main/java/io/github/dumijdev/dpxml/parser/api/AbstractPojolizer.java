package io.github.dumijdev.dpxml.parser.api;

import io.github.dumijdev.dpxml.parser.exception.XmlProcessingException;
import io.github.dumijdev.dpxml.parser.model.Node;
import io.github.dumijdev.dpxml.parser.serializer.XmlDeserializer;

import java.awt.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractPojolizer implements Pojolizer {
  private static final List<Class<?>> PRIMITIVE_TYPES;
  private final Map<Class<?>, XmlDeserializer<?>> deserializers = new HashMap<>();


  static {
    PRIMITIVE_TYPES = List.of(
        int.class, Integer.class,
        char.class, Character.class,
        long.class, Long.class,
        double.class, Double.class,
        float.class, Float.class,
        boolean.class, Boolean.class,
        byte.class, Byte.class,
        short.class, Short.class
    );
  }

  public AbstractPojolizer() {
    initializeDefaultDeserializers();
  }

  public void addDeserializer(Class<?> clazz, XmlDeserializer<?> xmlDeserializer) {
    this.deserializers.put(clazz, xmlDeserializer);
  }

  public XmlDeserializer<?> getDeserializer(Class<?> clazz) {
    return deserializers.computeIfAbsent(clazz, aClass -> {
      try {
        var constructor =  aClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return (XmlDeserializer<?>) constructor.newInstance();
      } catch (Exception e) {
        throw new RuntimeException("Failed to instantiate deserializer: " + aClass.getName(), e);
      }
    });
  }

  public boolean isPrimitiveOrString(Class<?> clazz) {
    return clazz.isPrimitive() || String.class.equals(clazz) || PRIMITIVE_TYPES.contains(clazz);
  }

  private void initializeDefaultDeserializers() {
    deserializers.put(String.class, s -> s);
    deserializers.put(Integer.class, Integer::parseInt);
    deserializers.put(int.class, Integer::parseInt);
    deserializers.put(Long.class, Long::parseLong);
    deserializers.put(long.class, Long::parseLong);
    deserializers.put(Double.class, Double::parseDouble);
    deserializers.put(double.class, Double::parseDouble);
    deserializers.put(Float.class, Float::parseFloat);
    deserializers.put(float.class, Float::parseFloat);
    deserializers.put(Boolean.class, Boolean::parseBoolean);
    deserializers.put(boolean.class, Boolean::parseBoolean);
    deserializers.put(Character.class, s -> s.isEmpty() ? null : s.charAt(0));
    deserializers.put(char.class, s -> s.isEmpty() ? '\0' : s.charAt(0));
    deserializers.put(Byte.class, Byte::parseByte);
    deserializers.put(byte.class, Byte::parseByte);
    deserializers.put(Short.class, Short::parseShort);
    deserializers.put(short.class, Short::parseShort);
  }

  public Object convertValue(String value, Class<?> targetType) {
    if (value == null || value.isEmpty()) {
      return getDefaultValue(targetType);
    }

    var converter = deserializers.get(targetType);
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

  private Object getDefaultValue(Class<?> type) {
    if (!PRIMITIVE_TYPES.contains(type)) return null;

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
}
