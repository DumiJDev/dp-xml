package io.github.dumijdev.dpxml.parser.api;

import io.github.dumijdev.dpxml.parser.serializer.XmlSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractXmlizer implements Xmlizer {
  private final Map<Class<?>, XmlSerializer<?>> serializers = new HashMap<>();

  public AbstractXmlizer() {
    initializeDefaultSerializers();
  }

  private void initializeDefaultSerializers() {
    addSerializer(String.class, (String val) -> val);
    addSerializer(int.class, Objects::toString);
    addSerializer(Integer.class, (Integer val) -> Integer.toString(val));
    addSerializer(float.class, (Float val) -> Float.toString(val));
    addSerializer(Float.class, (Float val) -> Float.toString(val));
    addSerializer(double.class, (Double val) -> Double.toString(val));
    addSerializer(Double.class, (Double val) -> Double.toString(val));
    addSerializer(long.class, (Long val) -> Long.toString(val));
    addSerializer(Long.class, (Long val) -> Long.toString(val));
    addSerializer(boolean.class, (Boolean val) -> Boolean.toString(val));
    addSerializer(Boolean.class, (Boolean val) -> Boolean.toString(val));
    addSerializer(String.class, (String val) -> val);
    addSerializer(Character.class, Object::toString);
    addSerializer(char.class, Object::toString);
    addSerializer(Byte.class, (Byte val) -> Byte.toString(val));
    addSerializer(byte.class, (Byte val) -> Byte.toString(val));
    addSerializer(Short.class, (Short val) -> Short.toString(val));
    addSerializer(short.class, (Short val) -> Short.toString(val));

    addSerializer(BigDecimal.class, (BigDecimal val) -> val.toPlainString());
    addSerializer(BigInteger.class, (BigInteger val) -> val.toString());
  }

  protected XmlSerializer<?> getSerializer(Class<?> clazz) {
    return serializers.get(clazz);
  }

  public void addSerializer(Class<?> clazz1, XmlSerializer<?> xmlSerializer) {
    serializers.put(clazz1, xmlSerializer);
  }
}
