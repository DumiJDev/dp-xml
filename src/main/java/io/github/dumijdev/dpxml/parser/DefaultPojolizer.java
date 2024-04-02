package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.model.Pojolizable;
import io.github.dumijdev.dpxml.stereotype.Element;
import io.github.dumijdev.dpxml.stereotype.IgnoreElement;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

import static io.github.dumijdev.dpxml.utils.ParserUtils.*;

public class DefaultPojolizer implements Pojolizer {

    private final DocumentBuilder documentBuilder;
    private final DateTimeFormatter dateFormatter;
    private final DateTimeFormatter dateTimeFormatter;

    public DefaultPojolizer() throws Exception {
        this.documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        this.dateTimeFormatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd[ HH[:mm[:ss[.S][.SS][.SSS]]]]")
                .toFormatter();
    }

    @Override
    public <T> T pojoify(String xml, Class<T> clazz) throws Exception {
        if (!clazz.isAnnotationPresent(Pojolizable.class)) {
            throw new Exception("Não é possível converter em POJO, classe: (" + clazz.getSimpleName() + ")");
        }
        var instance = clazz.getDeclaredConstructor().newInstance();

        var element = documentBuilder.parse(new InputSource(new StringReader(xml))).getDocumentElement();

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
            var children = findNodes(element, name);

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

                for (var item : children) {
                    if (isPrimitive(actualTypeArgument)) {
                        values.add(convertValue((org.w3c.dom.Element) item, actualTypeArgument));
                    } else {

                        if (!actualTypeArgument.isAnnotationPresent(Pojolizable.class)) {
                            throw new Exception();
                        }

                        var obj = pojoify(stringifyXml(item), actualTypeArgument);

                        values.add(obj);
                    }
                }

                field.set(instance, values);
            } else {
                if (field.getType().isAnnotationPresent(Pojolizable.class)) {
                    var node = findNode(element, name);

                    if (Objects.isNull(node)) {
                        continue;
                    }

                    var obj = pojoify(stringifyXml(node), field.getType());

                    field.set(instance, obj);
                } else throw new Exception("");
            }

        }

        return instance;
    }

    private void setValue(Field field, Object instance, List<Node> children) throws IllegalAccessException {
        if (children.isEmpty()) {
            return;
        }

        for (var node : children) {
            var child = (org.w3c.dom.Element) node;
            field.set(instance, convertValue(child, field.getType()));
        }
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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(child.getTextContent(), formatter);
        } else {
            return null;
            //throw new UnsupportedOperationException("Conversão para o tipo " + targetType + " não suportada.");
        }
    }

}


