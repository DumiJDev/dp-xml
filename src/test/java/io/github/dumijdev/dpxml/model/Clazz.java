package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.Element;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Pojolizable
@ToString
public class Clazz {
    @Element(namespace = "pay1")
    private List<Person> person;
}
