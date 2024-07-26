package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Pojolizable
@DeclaredNamespaces(aliases = {"ns3", "ns4"})
//@Namespaces(namespaces = {@Namespace(value = "https://1234.tes", name = "ns3"), @Namespace(value = "https://1234.tes", name = "ns4")})
@RootElement(namespace = "ns3")
@Xmlizable
@EqualsAndHashCode
@ToString
public class Company {
  private String id;
  @Element(namespace = "tst")
  private Employee employee;
  @Element(name = "name", namespace = "tst")
  private Set<String> names;
}
