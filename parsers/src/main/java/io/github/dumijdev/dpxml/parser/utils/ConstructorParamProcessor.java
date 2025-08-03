package io.github.dumijdev.dpxml.parser.utils;

import io.github.dumijdev.dpxml.annotations.Element;
import io.github.dumijdev.dpxml.annotations.IgnoreElement;

import javax.xml.stream.XMLStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConstructorParamProcessor {
  public <T> void processParametersWithMap(T instance, XMLStreamReader reader, XmlContext context,
                                           Map<String, Parameter> parameterMap) throws Exception {}
  public Map<String, Parameter> createParametersMap(Constructor<?> constructor) {
    return Arrays.stream(constructor.getParameters())
        .filter(parameter -> !parameter.isAnnotationPresent(IgnoreElement.class))
        .collect(Collectors.toMap(
            this::getParamName,
            Function.identity(),
            (existing, replacement) -> existing
        ));
  }

  private String getParamName(Parameter param) {
    if (param.isAnnotationPresent(Element.class)) {
      var annotation = param.getAnnotation(Element.class);
      return annotation.name().isEmpty() ? param.getName() : annotation.name();
    }

    return param.getName();
  }


}
