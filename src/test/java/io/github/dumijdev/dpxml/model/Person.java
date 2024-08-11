package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.*;
import lombok.*;

@Data
@Pojolizable
@Xmlizable
@StaticAttribute(name = "classType", value = "Class")
@DynamicAttribute(name = "dynamicClass", method = "name")
public class Person {
  @StaticAttribute(name = "staticType", value = "String")
  @DynamicAttribute(name = "dynamicType", method = "name")
  private String name;
  private int age;

  public Person() {
  }

  @DynamicAttributeGenerator(name = "generated", fields = {"age", "name"})
  public String name() {
    return name;
  }
}