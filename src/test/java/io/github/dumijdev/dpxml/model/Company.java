package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;


@Pojolizable
@DeclaredNamespaces(aliases = {"ns3", "ns4"})
//@Namespaces(namespaces = {@Namespace(value = "https://1234.tes", name = "ns3"), @Namespace(value = "https://1234.tes", name = "ns4")})
@RootElement(namespace = "ns3")
@Xmlizable
public class Company {
    private String id;
    @Element(namespace = "ns4")
    private Employees employees;
    @Element(name = "name", namespace = "tst")
    private Set<String> names;

    public Company() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Employees getEmployees() {
        return employees;
    }

    public void setEmployees(Employees employees) {
        this.employees = employees;
    }

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
    }

    @Override
    public String toString() {
        return "Company{" +
            "id='" + id + '\'' +
            ", employees=" + employees +
            ", names=" + names +
            '}';
    }

    @Pojolizable
    @Xmlizable
    public static class Employees {
        @Element(namespace = "tst")
        private List<Employee> employee;

        public Employees() {
        }

        @Override
        public String toString() {
            return "Employees{" +
                "employee=" + employee +
                '}';
        }
    }
}
