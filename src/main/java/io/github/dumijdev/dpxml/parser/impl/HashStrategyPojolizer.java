package io.github.dumijdev.dpxml.parser.impl;

import io.github.dumijdev.dpxml.enums.HashStrategyType;
import io.github.dumijdev.dpxml.parser.Pojolizer;
import io.github.dumijdev.dpxml.stereotype.IgnoreElement;
import io.github.dumijdev.dpxml.stereotype.Pojolizable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import static io.github.dumijdev.dpxml.utils.ParserUtils.*;

public class HashStrategyPojolizer implements Pojolizer {

  private final DocumentBuilder documentBuilder;
  private final DateTimeFormatter dateFormatter;
  private final DateTimeFormatter dateTimeFormatter;
  private HashStrategyType type;

  private HashStrategyPojolizer() throws Exception {
    this.documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    this.dateTimeFormatter = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd[ HH[:mm[:ss[.S][.SS][.SSS]]]]")
        .toFormatter();
    this.type = HashStrategyType.SEQUENTIAL;
  }

  private HashStrategyPojolizer(HashStrategyType type) throws Exception {
    this();
    this.type = type;
  }

  public static HashStrategyPojolizer newInstance() throws Exception {
    return new HashStrategyPojolizer();
  }

  public static HashStrategyPojolizer newInstance(HashStrategyType type) throws Exception {
    return new HashStrategyPojolizer(type);
  }

  @Override
  public synchronized <T> T pojoify(String xml, Class<T> clazz) throws Exception {
    if (!clazz.isAnnotationPresent(Pojolizable.class)) {
      throw new Exception("Não é possível converter em POJO, classe: (" + clazz.getSimpleName() + ")");
    }
    var instance = clazz.getDeclaredConstructor().newInstance();

    var simpleNode = type == HashStrategyType.PARALLEL ? new ConcurrentHashMap<String, Node>() : new HashMap<String, Node>();
    var repeatedNode = type == HashStrategyType.PARALLEL ? new ConcurrentHashMap<String, List<Node>>() : new HashMap<String, List<Node>>();

    var element = documentBuilder.parse(new InputSource(new StringReader(xml))).getDocumentElement();

    preProcessXml(element, simpleNode, repeatedNode);

    var stream = Arrays.stream(clazz.getDeclaredFields());

    if (type == HashStrategyType.PARALLEL)
      stream = stream.parallel();

    stream.forEachOrdered(field -> {
      try {
        if (field.isAnnotationPresent(IgnoreElement.class))
          return;

        field.setAccessible(true);

        String name = "";

        var elementAnnotation = field.getAnnotation(io.github.dumijdev.dpxml.stereotype.Element.class);
        if (elementAnnotation != null) {
          name = elementAnnotation.name();
        }

        name = name.isEmpty() ? field.getName() : name;

        var fieldType = field.getType();

        if (!simpleNode.containsKey(name) && !repeatedNode.containsKey(name)) return;


        if (isPrimitive(fieldType)) {
          if (!simpleNode.containsKey(name))
            return;
          var value = convertValue((Element) simpleNode.get(name), fieldType);
          field.set(instance, value);
        } else if (isCollection(fieldType)) {
          var parameterizedType = (ParameterizedType) field.getGenericType();
          var actualTypeArgument = Class.forName(parameterizedType.getActualTypeArguments()[0].getTypeName());
          Collection<Object> values;
          if (isSet(fieldType)) {
            values = new LinkedHashSet<>();
          } else {
            values = new LinkedList<>();
          }
          if (repeatedNode.containsKey(name)) {
            for (var item : repeatedNode.get(name)) {
              if (isPrimitive(actualTypeArgument)) {
                values.add(convertValue((org.w3c.dom.Element) item, actualTypeArgument));
              } else {
                if (!actualTypeArgument.isAnnotationPresent(Pojolizable.class)) {
                  throw new Exception(String.format("Campo %s não está marcado como pojolizable", name));
                }
                var obj = pojoify(stringifyXml(item), actualTypeArgument);
                values.add(obj);
              }
            }
          }
          field.set(instance, values);
        } else {
          if (fieldType.isAnnotationPresent(Pojolizable.class)) {
            var node = simpleNode.get(name);
            if (node != null) {
              var obj = pojoify(stringifyXml(node), fieldType);
              field.set(instance, obj);
            }
          } else throw new Exception(String.format("Campo %s não está marcado como pojolizable", name));
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    return instance;
  }

  @Override
  public <T> T pojoify(io.github.dumijdev.dpxml.model.Node node, Class<T> clazz) throws Exception {
    return null;
  }

  private Object convertValue(org.w3c.dom.Element child, Class<?> targetType) {
    if (targetType.equals(String.class)) {
      return child.getTextContent();
    } else if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
      return Integer.parseInt(child.getTextContent());
    } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
      return Long.parseLong(child.getTextContent());
    } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
      return Double.parseDouble(child.getTextContent());
    } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
      return Float.parseFloat(child.getTextContent());
    } else if (targetType.equals(Short.class) || targetType.equals(short.class)) {
      return Short.parseShort(child.getTextContent());
    } else if (targetType.equals(Byte.class) || targetType.equals(byte.class)) {
      return Byte.parseByte(child.getTextContent());
    } else if (targetType.equals(Character.class) || targetType.equals(char.class)) {
      if (child.getTextContent().length() == 1) {
        return child.getTextContent().charAt(0);
      } else {
        throw new IllegalArgumentException("A string de entrada não é um caractere válido para conversão.");
      }
    } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
      return Boolean.parseBoolean(child.getTextContent());
    } else if (targetType.equals(LocalDateTime.class)) {
      if (child.getTextContent().matches("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}")) {
        return LocalDate.parse(child.getTextContent(), dateFormatter).atStartOfDay();
      }
      return LocalDateTime.parse(child.getTextContent(), dateTimeFormatter);
    } else if (targetType.equals(Date.class)) {
      throw new UnsupportedOperationException();
    } else if (targetType.equals(LocalDate.class)) {
      return LocalDate.parse(child.getTextContent(), dateFormatter);
    } else {
      return null;
    }
  }

  private void preProcessXml(Element element, Map<String, Node> simpleNode, Map<String, List<Node>> repeatedNode) {
    var children = element.getChildNodes();

    IntStream stream = IntStream.range(0, children.getLength());

    if (type == HashStrategyType.PARALLEL)
      stream = stream.parallel();

    stream.forEachOrdered(i -> {
      var item = children.item(i);
      if (item.getNodeType() == Node.ELEMENT_NODE) {
        var name = simpleNodeName(item.getNodeName());
        if (repeatedNode.containsKey(name)) {
          repeatedNode.get(name).add(item);
        } else if (simpleNode.containsKey(name)) {
          var items = new CopyOnWriteArrayList<Node>();
          var node = simpleNode.remove(name);
          items.add(node);
          items.add(item);
          repeatedNode.put(name, items);
        } else {
          simpleNode.put(name, item);
        }
      }
    });
  }
}
