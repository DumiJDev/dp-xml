package io.github.dumijdev.dpxml.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Pojolizable
@Xmlizable
@EqualsAndHashCode
public class Person {
    private String name;
    private int age;

    public Person() {
    }
}