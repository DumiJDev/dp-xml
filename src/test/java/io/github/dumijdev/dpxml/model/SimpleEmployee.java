package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.Element;
import io.github.dumijdev.dpxml.stereotype.FlexElement;
import io.github.dumijdev.dpxml.stereotype.Pojolizable;

import java.util.List;
import java.util.Objects;

@Pojolizable
public class SimpleEmployee {
    @FlexElement(src = "person.name", dst = "name1")
    @Element(name = "name1")
    private String name;
    @FlexElement(src = "person.age", dst = "age1")
    @Element(name = "age1")
    private int age;
    @FlexElement(src = "person.names.name", dst = "tempName")
    private List<String> tempName;

    public SimpleEmployee() {
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public List<String> getTempName() {
        return tempName;
    }

    public void setTempName(List<String> tempName) {
        this.tempName = tempName;
    }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (!(object instanceof SimpleEmployee)) return false;
    SimpleEmployee that = (SimpleEmployee) object;
    return age == that.age && Objects.equals(name, that.name) && Objects.equals(tempName, that.tempName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, age, tempName);
  }

  @Override
    public String toString() {
        return "SimpleEmployee{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", tempName=" + tempName +
                '}';
    }
}
