package io.github.dumijdev.dpxml.parser.utils;


import io.github.dumijdev.dpxml.annotations.*;
import io.github.dumijdev.dpxml.parser.exception.InvalidReferenceNameException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class Attributes {
    public static Map<String, String> getAttributes(Class<?> clazz, Object obj) throws InvocationTargetException, IllegalAccessException {
        var attributes = new HashMap<String, List<String>>();
        //Class process
        if (clazz.isAnnotationPresent(StaticAttribute.class)) {
            var attribute = clazz.getDeclaredAnnotation(StaticAttribute.class);
            addAttributeToMap(attributes, clazz.getName(), attribute.name(), attribute.value());
        }

        if (clazz.isAnnotationPresent(StaticAttributes.class)) {
            for (var attr : clazz.getDeclaredAnnotation(StaticAttributes.class).attributes()) {
                addAttributeToMap(attributes, clazz.getName(), attr.name(), attr.value());
            }
        }

        if (clazz.isAnnotationPresent(DynamicAttribute.class)) {
            var attribute = clazz.getDeclaredAnnotation(DynamicAttribute.class);
            addAttributeToMap(clazz, obj, attributes, clazz.getName(), attribute);
        }

        if (clazz.isAnnotationPresent(DynamicAttributes.class)) {
            for (var attr : clazz.getDeclaredAnnotation(DynamicAttributes.class).attributes()) {
                addAttributeToMap(clazz, obj, attributes, clazz.getName(), attr);
            }
        }


        //Fields process
        for (var attr : clazz.getDeclaredFields()) {
            if (attr.isAnnotationPresent(StaticAttribute.class)) {
                var staticAttr = attr.getDeclaredAnnotation(StaticAttribute.class);
                addAttributeToMap(attributes, attr.getName(), staticAttr.name(), staticAttr.value());
            }

            if (attr.isAnnotationPresent(StaticAttributes.class)) {
                for (var staticAttr : attr.getDeclaredAnnotation(StaticAttributes.class).attributes()) {
                    addAttributeToMap(attributes, attr.getName(), staticAttr.name(), staticAttr.value());
                }
            }

            if (attr.isAnnotationPresent(DynamicAttribute.class)) {
                var dynamicAttr = attr.getDeclaredAnnotation(DynamicAttribute.class);
                addAttributeToMap(clazz, obj, attributes, attr.getName(), dynamicAttr);
            }

            if (attr.isAnnotationPresent(DynamicAttributes.class)) {
                for (var dynamicAttr : attr.getDeclaredAnnotation(DynamicAttributes.class).attributes()) {
                    addAttributeToMap(clazz, obj, attributes, attr.getName(), dynamicAttr);
                }
            }
        }

        //Method process
        for (var method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(DynamicAttributeGenerator.class)) {
                var dag = method.getDeclaredAnnotation(DynamicAttributeGenerator.class);

                for (var fieldName : dag.fields()) {
                    Field field;
                    try {
                        field = clazz.getDeclaredField(fieldName);
                    } catch (NoSuchFieldException e) {
                        throw new InvalidReferenceNameException(e);
                    }
                    addAttributeToMap(attributes, field.getName(), dag.name(), method.invoke(obj).toString());
                }
            }
        }

        return adapt(attributes);
    }

    private static Map<String, String> adapt(Map<String, List<String>> attributes) {
        Map<String, String> map = new HashMap<>();

        attributes.forEach((key, value) -> map.put(key, value.toString().replaceAll("[\\[\\],]", "")));

        return map;
    }

    private static void addAttributeToMap(Class<?> clazz, Object obj, HashMap<String, List<String>> attributes, String fieldName, DynamicAttribute dynamicAttr) throws IllegalAccessException, InvocationTargetException {
        Method method;
        try {
            method = clazz.getDeclaredMethod(dynamicAttr.method());
        } catch (NoSuchMethodException e) {
            throw new InvalidReferenceNameException(e);
        }

        method.trySetAccessible();
        var value = method.invoke(obj).toString();

        addAttributeToMap(attributes, fieldName, dynamicAttr.name(), value);
    }

    private static void addAttributeToMap(HashMap<String, List<String>> attributes, String fieldName, String attrName, String value) {
        var attrString = String.format("%s=\"%s\"", attrName, value);
        if (!attributes.containsKey(fieldName)) {
            var list = new LinkedList<String>();

            list.add(attrString);

            attributes.put(fieldName, list);
        } else {
            attributes.get(fieldName).add(attrString);
        }
    }

}
