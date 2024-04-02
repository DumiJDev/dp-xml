package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.Element;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Pojolizable
@Xmlizable
@ToString
public class Employee {
    private String id;
    @Element(namespace = "pay")
    private Person person;
}
