package io.github.dumijdev.dpxml.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Pojolizable
public class Employee {
    private String id;
    private Person person;
}
