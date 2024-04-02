package io.github.dumijdev.dpxml.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Pojolizable
@Xmlizable
@EqualsAndHashCode
@ToString
public class Person {
    private String name;
    private int age;

    public Person() {
    }
}