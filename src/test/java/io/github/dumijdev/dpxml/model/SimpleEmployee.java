package io.github.dumijdev.dpxml.model;

import io.github.dumijdev.dpxml.stereotype.FlexElement;
import io.github.dumijdev.dpxml.stereotype.Pojolizable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Pojolizable
@NoArgsConstructor
@Data
@ToString
public class SimpleEmployee {
  @FlexElement(src = "person.name", dst = "name")
  private String name;
  @FlexElement(src = "person.age", dst = "age")
  private int age;


}
