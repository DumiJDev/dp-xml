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
import java.lang.reflect.ParameterizedType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.github.dumijdev.dpxml.utils.ParserUtils.*;

/**
 * Implementation of Pojolizer that converts XML to POJOs.
 */
public class BasicPojolizer implements Pojolizer {

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd[ HH[:mm[:ss[.S][.SS][.SSS]]]]";
    private static final String NUMERIC_REGEX = "\\d+";
    private static final String DATE_REGEX = "\\d{4}-\\d{1,2}-\\d{1,2}";

    private final DateTimeFormatter dateFormatter;
    private final DateTimeFormatter dateTimeFormatter;
    private final ThreadLocal<Nodilizer> nodilizer;
    private final Map<Class<?>, ValueConverter<?>> typeConverters;

    /**
     * Creates a new instance of BasicPojolizer with default formatters.
     */
    public BasicPojolizer() {
        this(DefaultNodilizer::new);
    }

    /**
     * Creates a new instance of BasicPojolizer with the given nodilizer supplier.
     *
     * @param nodilizerSupplier The supplier for creating Nodilizer instances
     */
    public BasicPojolizer(Supplier<Nodilizer> nodilizerSupplier) {
        this.dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        this.dateTimeFormatter = new DateTimeFormatterBuilder()
            .appendPattern(DATE_TIME_PATTERN)
            .toFormatter();
        this.nodilizer = ThreadLocal.withInitial(nodilizerSupplier);
        this.typeConverters = initializeTypeConverters();
    }

    private Map<Class<?>, ValueConverter<?>> initializeTypeConverters() {
        Map<Class<?>, ValueConverter<?>> converters = new HashMap<>();

        // Primitive types
        converters.put(String.class, value -> value);
        converters.put(Integer.class, Integer::parseInt);
        converters.put(int.class, Integer::parseInt);
        converters.put(Long.class, Long::parseLong);
        converters.put(long.class, Long::parseLong);
        converters.put(Double.class, Double::parseDouble);
        converters.put(double.class, Double::parseDouble);
        converters.put(Float.class, Float::parseFloat);
        converters.put(float.class, Float::parseFloat);
        converters.put(Short.class, Short::parseShort);
        converters.put(short.class, Short::parseShort);
        converters.put(Byte.class, Byte::parseByte);
        converters.put(byte.class, Byte::parseByte);
        converters.put(Boolean.class, Boolean::parseBoolean);
        converters.put(boolean.class, Boolean::parseBoolean);

        // Complex types
        converters.put(Character.class, value -> {
            if (value.length() == 1) return value.charAt(0);
            throw new IllegalArgumentException("Input string is not a valid character for conversion.");
        });
        converters.put(char.class, value -> {
            if (value.length() == 1) return value.charAt(0);
            throw new IllegalArgumentException("Input string is not a valid character for conversion.");
        });

        converters.put(LocalDateTime.class, value -> {
            if (value.matches(DATE_REGEX)) {
                return LocalDate.parse(value, dateFormatter).atStartOfDay();
            }
            return LocalDateTime.parse(value, dateTimeFormatter);
        });

        converters.put(LocalDate.class, value -> LocalDate.parse(value, dateFormatter));

        converters.put(Date.class, value -> {
            if (value.matches(NUMERIC_REGEX)) {
                return new Date(Long.parseLong(value));
            }
            throw new IllegalArgumentException(String.format("Cannot convert %s to Date type", value));
        });

        return Collections.unmodifiableMap(converters);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T pojoify(String xml, Class<T> clazz) {
        if (String.class.equals(clazz)) {
            return (T) xml;
        }

        validatePojolizable(clazz);

        Node node = nodilizer.get().nodify(xml);
        var pojo = pojoify(node, clazz);

        nodilizer.remove();
        return pojo;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T pojoify(Node node, Class<T> clazz) {
        try {
            if (String.class.equals(clazz)) {
                return (T) node.asXml();
            }

            validatePojolizable(clazz);

            T instance = clazz.getDeclaredConstructor().newInstance();
            populateFields(node, clazz, instance);

            return instance;
        } catch (ReflectiveOperationException e) {
            throw new InternalErrorException(e);
        }
    }

    private <T> void validatePojolizable(Class<T> clazz) {
        if (!clazz.isAnnotationPresent(Pojolizable.class)) {
            throw new UnPojolizableException(clazz.getSimpleName());
        }
    }

    private <T> void populateFields(Node node, Class<T> clazz, T instance)
        throws ReflectiveOperationException {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(IgnoreElement.class) || !field.trySetAccessible()) {
                continue;
            }

            String elementName = getElementName(field);
            populateField(node, instance, field, elementName);
        }
    }

    private <T> void populateField(Node node, T instance, Field field, String elementName)
        throws ReflectiveOperationException {
        Class<?> fieldType = field.getType();

        if (isPrimitive(fieldType)) {
            populatePrimitiveField(node, instance, field, elementName);
        } else if (isCollection(fieldType)) {
            populateCollectionField(node, instance, field, elementName);
        } else if (fieldType.isAnnotationPresent(Pojolizable.class)) {
            populateComplexField(node, instance, field, elementName);
        } else {
            throw new UnPojolizableException(fieldType.getSimpleName());
        }
    }

    private <T> void populatePrimitiveField(Node node, T instance, Field field, String elementName)
        throws IllegalAccessException {
        Node childNode = node.child(elementName);
        Object value = convertValue(childNode.content(), field.getType());

        if (value != null) {
            field.set(instance, value);
        }
    }

    private <T> void populateCollectionField(Node node, T instance, Field field, String elementName)
        throws ReflectiveOperationException {
        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
        Class<?> genericType = Class.forName(parameterizedType.getActualTypeArguments()[0].getTypeName());

        Collection<Object> values = createCollectionInstance(field.getType());
        List<Node> children = node.children(elementName);

        if (children != null && !children.isEmpty()) {
            List<Object> convertedValues = children.stream()
                .filter(childNode -> !childNode.isMissing())
                .map(childNode -> convertNodeToValue(childNode, genericType))
                .collect(Collectors.toList());

            values.addAll(convertedValues);
        }

        field.set(instance, values);
    }

    private Collection<Object> createCollectionInstance(Class<?> collectionType) {
        return isSet(collectionType) ? new LinkedHashSet<>() : new ArrayList<>();
    }

    private Object convertNodeToValue(Node node, Class<?> type) {
        try {
            if (isPrimitive(type)) {
                return convertValue(node.content(), type);
            } else {
                validatePojolizable(type);
                return pojoify(node, type);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error converting node to value", e);
        }
    }

    private <T> void populateComplexField(Node node, T instance, Field field, String elementName)
        throws IllegalAccessException {
        Node childNode = node.child(elementName);

        if (childNode.isMissing()) {
            return;
        }

        Object value = pojoify(childNode, field.getType());
        field.set(instance, value);
    }

    private String getElementName(Field field) {
        if (field.isAnnotationPresent(Element.class)) {
            Element metadata = field.getAnnotation(Element.class);
            String name = metadata.name();

            if (name != null && !name.isEmpty()) {
                return name;
            }
        }

        return field.getName();
    }

    @SuppressWarnings("unchecked")
    private <T> T convertValue(String value, Class<T> type) {
        if (value == null) {
            return null;
        }

        ValueConverter<T> converter = (ValueConverter<T>) typeConverters.get(type);
        if (converter != null) {
            return converter.convert(value);
        }

        return null;
    }

    /**
     * Functional interface for converting string values to specific types.
     */
    @FunctionalInterface
    private interface ValueConverter<T> {
        T convert(String value);
    }
}