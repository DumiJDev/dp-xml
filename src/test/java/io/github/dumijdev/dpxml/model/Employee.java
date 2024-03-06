package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.Element;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Pojolizable
@Xmlizable
public class Employee {
    private String id;
    @Element(namespace = "pay")
    private Person person;

    @Override
    public String toString() {
        return "Employee{" +
                "id='" + id + '\'' +
                ", person=" + person +
                '}';
    }
}
