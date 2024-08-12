package io.github.dumijdev.dpxml.parser.impl.pojo;

import io.github.dumijdev.dpxml.exception.InternalErrorException;
import io.github.dumijdev.dpxml.exception.UnPojolizableException;
import io.github.dumijdev.dpxml.model.Node;
import io.github.dumijdev.dpxml.parser.Nodilizer;
import io.github.dumijdev.dpxml.parser.Pojolizer;
import io.github.dumijdev.dpxml.parser.impl.node.DefaultNodilizer;
import io.github.dumijdev.dpxml.stereotype.Element;
import io.github.dumijdev.dpxml.stereotype.IgnoreElement;
import io.github.dumijdev.dpxml.stereotype.Pojolizable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.dumijdev.dpxml.utils.ParserUtils.*;


public class BasicPojolizer implements Pojolizer {
    private final DateTimeFormatter dateFormatter;
    private final DateTimeFormatter dateTimeFormatter;
    private final ThreadLocal<Nodilizer> nodilizer = ThreadLocal.withInitial(DefaultNodilizer::new);

    public BasicPojolizer() {
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        this.dateTimeFormatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd[ HH[:mm[:ss[.S][.SS][.SSS]]]]")
                .toFormatter();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T pojoify(String xml, Class<T> clazz) {
        if (String.class.equals(clazz)) {

            return (T) xml;
        }

        if (!clazz.isAnnotationPresent(Pojolizable.class)) {
            throw new UnPojolizableException(clazz.getSimpleName());
        }

        var node = nodilizer.get().nodify(xml);

        return pojoify(node, clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T pojoify(Node node, Class<T> clazz) {
        try {
            if (String.class.equals(clazz)) {

                return (T) node.asXml();
            }

            if (!clazz.isAnnotationPresent(Pojolizable.class)) {
                throw new UnPojolizableException(clazz.getSimpleName());
            }

            var instance = clazz.getDeclaredConstructor().newInstance();

            for (var field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(IgnoreElement.class) || !field.trySetAccessible())
                    continue;

                var name = getName(field);

                if (isPrimitive(field.getType())) {
                    var child = node.child(name);
                    var value = convertValue(child.content(), field.getType());
                    if (Objects.nonNull(value))
                        field.set(instance, value);
                } else if (isCollection(field.getType())) {
                    var parameterizedType = (ParameterizedType) field.getGenericType();
                    var actualTypeArgument = Class.forName(parameterizedType.getActualTypeArguments()[0].getTypeName());

                    Collection<Object> values;

                    if (isSet(field.getType())) {
                        values = new LinkedHashSet<>();
                    } else {
                        values = new LinkedList<>();
                    }

                    var children = node.children(name);

                    if (Objects.nonNull(children) && !children.isEmpty()) {
                        values.addAll(children
                                .stream()
                                .filter(node1 -> !node1.isMissing())
                                .map(node1 -> {
                                    try {
                                        if (isPrimitive(actualTypeArgument)) {
                                            System.out.println("Content: " + node1.content());
                                            return convertValue(node1.content(), actualTypeArgument);
                                        } else {

                                            if (!actualTypeArgument.isAnnotationPresent(Pojolizable.class)) {
                                                throw new UnPojolizableException(clazz.getSimpleName());
                                            }

                                            return pojoify(node1, actualTypeArgument);
                                        }
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .collect(Collectors.toList()));

                    }
                    field.set(instance, values);
                } else {
                    if (field.getType().isAnnotationPresent(Pojolizable.class)) {
                        var localNode = node.child(name);

                        if (localNode.isMissing()) {
                            continue;
                        }

                        var obj = pojoify(localNode, field.getType());

                        field.set(instance, obj);
                    } else throw new UnPojolizableException(field.getType().getSimpleName());
                }

            }

            return instance;

        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new InternalErrorException(e);
        }
    }

    private String getName(Field field) {
        String name = null;

        if (field.isAnnotationPresent(Element.class)) {
            var metadata = field.getAnnotation(Element.class);

            name = metadata.name();
        }

        name = Objects.isNull(name) || name.isEmpty() ? field.getName() : name;

        return name;
    }

    private Object convertValue(String value, Class<?> type) {
        if (Objects.isNull(value)) return null;

        if (type.equals(String.class)) return value;
        else if (type.equals(Integer.class) || type.equals(int.class)) return Integer.parseInt(value);
        else if (type.equals(Long.class) || type.equals(long.class)) return Long.parseLong(value);
        else if (type.equals(Double.class) || type.equals(double.class)) return Double.parseDouble(value);
        else if (type.equals(Float.class) || type.equals(float.class)) return Float.parseFloat(value);
        else if (type.equals(Short.class) || type.equals(short.class)) return Short.parseShort(value);
        else if (type.equals(Byte.class) || type.equals(byte.class)) return Byte.parseByte(value);
        else if (type.equals(Character.class) || type.equals(char.class)) {
            if (value.length() == 1) return value.charAt(0);
            else throw new IllegalArgumentException("A string de entrada não é um caractere válido para conversão.");
        } else if (type.equals(Boolean.class) || type.equals(boolean.class))
            return Boolean.parseBoolean(value);
        else if (type.equals(LocalDateTime.class)) {

            if (value.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                return LocalDate.parse(value, dateFormatter).atStartOfDay();
            }

            return LocalDateTime.parse(value, dateTimeFormatter);
        } else if (type.equals(Date.class)) {
            if (value.matches("\\d+")) {
                return new Date(Long.parseLong(value));
            } else
                throw new IllegalArgumentException(String.format("Não é possível converter %s para o tipo Date", value));
        } else if (type.equals(LocalDate.class)) {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } else {
            return null;
        }
    }
}
