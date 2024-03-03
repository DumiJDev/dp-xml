package io.github.dumijdev.dpxml.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Pojolizable
public class Person {
    private String name;
    private int age;

    public Person() {
    }
}