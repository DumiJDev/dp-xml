package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.Element;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Pojolizable
@EqualsAndHashCode
public class Company {
    private String id;
    @Element(name = "employee")
    private Set<Employee> employees;
}
