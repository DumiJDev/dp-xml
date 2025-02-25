package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.Element;
import io.github.dumijdev.dpxml.stereotype.FlexElement;
import io.github.dumijdev.dpxml.stereotype.Pojolizable;

import java.util.List;

@Pojolizable
public class SimpleEmployee {
    @FlexElement(src = "person.name", dst = "name1")
    @Element(name = "name1")
    private String name;
    @FlexElement(src = "person.age", dst = "age1")
    @Element(name = "age1")
    private List<Integer> ages;
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

  public List<Integer> getAges() {
    return ages;
  }

  public void setAges(List<Integer> ages) {
    this.ages = ages;
  }

  public List<String> getTempName() {
        return tempName;
    }

    public void setTempName(List<String> tempName) {
        this.tempName = tempName;
    }

  @Override
  public String toString() {
    return "SimpleEmployee{" +
        "name='" + name + '\'' +
        ", ages=" + ages +
        ", tempName=" + tempName +
        '}';
  }
}
