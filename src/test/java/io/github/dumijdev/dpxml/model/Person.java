package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.*;

import java.util.Objects;
import java.util.UUID;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @DynamicAttributeGenerator(name = "generated", fields = {"age", "name"})
    public String name() {
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Person)) return false;
        Person person = (Person) object;
        return age == person.age && Objects.equals(name, person.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}