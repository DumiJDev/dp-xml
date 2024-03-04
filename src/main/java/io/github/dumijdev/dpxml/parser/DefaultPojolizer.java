package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.model.Pojolizable;
import io.github.dumijdev.dpxml.stereotype.Element;
import io.github.dumijdev.dpxml.stereotype.IgnoreElement;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.Temporal;
import java.util.*;

public class DefaultPojolizer implements Pojolizer {
    private static final Set<Class<?>> CONSIDERED_PRIMITIVES = new HashSet<>();

    static {
        CONSIDERED_PRIMITIVES.add(Integer.class);
        CONSIDERED_PRIMITIVES.add(Long.class);
        CONSIDERED_PRIMITIVES.add(Byte.class);
        CONSIDERED_PRIMITIVES.add(Short.class);
        CONSIDERED_PRIMITIVES.add(Float.class);
        CONSIDERED_PRIMITIVES.add(Double.class);
        CONSIDERED_PRIMITIVES.add(Boolean.class);
        CONSIDERED_PRIMITIVES.add(Character.class);

        CONSIDERED_PRIMITIVES.add(String.class);

        CONSIDERED_PRIMITIVES.add(Date.class);
        CONSIDERED_PRIMITIVES.add(java.sql.Date.class);
        CONSIDERED_PRIMITIVES.add(Temporal.class);

        CONSIDERED_PRIMITIVES.add(int.class);
        CONSIDERED_PRIMITIVES.add(long.class);
        CONSIDERED_PRIMITIVES.add(byte.class);
        CONSIDERED_PRIMITIVES.add(short.class);
        CONSIDERED_PRIMITIVES.add(float.class);
        CONSIDERED_PRIMITIVES.add(double.class);
        CONSIDERED_PRIMITIVES.add(boolean.class);
        CONSIDERED_PRIMITIVES.add(char.class);
    }

    @Override
    public <T> T convert(String xml, Class<T> clazz) throws Exception {
        if (!clazz.isAnnotationPresent(Pojolizable.class)) {
            throw new Exception();
        }
        var instance = clazz.getDeclaredConstructor().newInstance();

        var element = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml))).getDocumentElement();

        for (var field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(IgnoreElement.class))
                continue;

            field.setAccessible(true);

            String name = "";

            if (field.isAnnotationPresent(Element.class)) {
                var metadata = field.getAnnotation(Element.class);

                name = metadata.name();
            }

            name = name.isEmpty() ? field.getName() : name;
            var children = element.getElementsByTagName(name);

            if (isPrimitive(field.getType())) {

                setValue(field, instance, children);

            } else if (isCollection(field.getType())) {
                var parameterizedType = (ParameterizedType) field.getGenericType();

                var actualTypeArgument = Class.forName(parameterizedType.getActualTypeArguments()[0].getTypeName());

                Collection<Object> values;

                if (isSet(field.getType())) {
                    values = new LinkedHashSet<>();
                } else {
                    values = new LinkedList<>();
                }

                for (var j = 0; j < children.getLength(); j++) {
                    var item = children.item(j);
                    System.out.println(j);
                    if (item.getNodeType() == Node.ELEMENT_NODE) {
                        if (isPrimitive(actualTypeArgument)) {
                            values.add(convertValue((org.w3c.dom.Element) item, actualTypeArgument));
                        } else {

                            if (!actualTypeArgument.isAnnotationPresent(Pojolizable.class)) {
                                System.out.println("Type: " + actualTypeArgument.getTypeName());
                                throw new Exception();
                            }

                            var obj = convert(stringifyXml(item), actualTypeArgument);

                            values.add(obj);
                        }
                    }
                }

                field.set(instance, values);
            } else {
                if (field.getType().isAnnotationPresent(Pojolizable.class)) {
                    var obj = convert(
                            stringifyXml(element.getParentNode()), field.getType()
                    );

                    field.set(instance, obj);
                } else throw new Exception("");
            }

        }

        return instance;
    }

    public String stringifyXml(Node node) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource domSource = new DOMSource(node);

        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);

        transformer.transform(domSource, result);

        return writer.toString();
    }

    private void setValue(Field field, Object instance, NodeList children) throws IllegalAccessException, NoSuchFieldException {
        if (children.getLength() < 1) {
            throw new NoSuchFieldException(String.format("Field %s, este campo não foi encontrado", field.getName()));
        }

        for (var i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                var child = (org.w3c.dom.Element) children.item(i);
                field.set(instance, convertValue(child, field.getType()));
            }
        }
    }


    private boolean isCollection(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz);
    }

    private boolean isList(Class<?> clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    private boolean isSet(Class<?> clazz) {
        return Set.class.isAssignableFrom(clazz);
    }

    private boolean isPrimitive(Class<?> clazz) {
        return CONSIDERED_PRIMITIVES.contains(clazz) || Temporal.class.isAssignableFrom(clazz);
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
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                return LocalDate.parse(child.getTextContent(), formatter).atStartOfDay();
            }

            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd[ HH[:mm[:ss[.S][.SS][.SSS]]]]")
                    .toFormatter();

            return LocalDateTime.parse(child.getTextContent(), formatter);
        } else if (targetType.equals(Date.class)) {
            throw new UnsupportedOperationException();
        } else if (targetType.equals(LocalDate.class)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(child.getTextContent(), formatter);
        } else {
            return null;
            //throw new UnsupportedOperationException("Conversão para o tipo " + targetType + " não suportada.");
        }
    }

}


