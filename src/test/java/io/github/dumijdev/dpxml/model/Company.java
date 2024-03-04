package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.Element;
import io.github.dumijdev.dpxml.stereotype.Namespace;
import io.github.dumijdev.dpxml.stereotype.Namespaces;
import io.github.dumijdev.dpxml.stereotype.RootElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Pojolizable
@Namespaces(namespaces = {@Namespace(value = "https://1234.tes", name = "ns3")})
@RootElement(namespace = "ns3")
@Xmlizable
@EqualsAndHashCode
public class Company {
    private String id;
    @Element(name = "employee")
    private Set<Employee> employees;
    @Element(name = "name", namespace = "")
    private Set<String> names;
}
