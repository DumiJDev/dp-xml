package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.Element;
import io.github.dumijdev.dpxml.stereotype.Pojolizable;

import java.util.List;
import java.util.Objects;

@Pojolizable
public class Clazz {
    @Element(namespace = "pay1")
    private List<Person> person;

    public Clazz() {
    }

    public List<Person> getPerson() {
        return person;
    }

    public void setPerson(List<Person> person) {
        this.person = person;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Clazz)) return false;
        Clazz clazz = (Clazz) object;
        return Objects.equals(person, clazz.person);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(person);
    }

    @Override
    public String toString() {
        return "Clazz{" +
                "person=" + person +
                '}';
    }
}
