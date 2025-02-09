package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.*;

import java.util.Objects;
import java.util.Set;


@Pojolizable
@DeclaredNamespaces(aliases = {"ns3", "ns4"})
//@Namespaces(namespaces = {@Namespace(value = "https://1234.tes", name = "ns3"), @Namespace(value = "https://1234.tes", name = "ns4")})
@RootElement(namespace = "ns3")
@Xmlizable
public class Company {
    private String id;
    @Element(namespace = "tst")
    private Employee employee;
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

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Company)) return false;
        Company company = (Company) object;
        return Objects.equals(id, company.id) && Objects.equals(employee, company.employee) && Objects.equals(names, company.names);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, employee, names);
    }

    @Override
    public String toString() {
        return "Company{" +
                "id='" + id + '\'' +
                ", employee=" + employee +
                ", names=" + names +
                '}';
    }
}
