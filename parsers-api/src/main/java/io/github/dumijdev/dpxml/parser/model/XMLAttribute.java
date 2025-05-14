package io.github.dumijdev.dpxml.parser.model;

public class XMLAttribute implements Node.Attribute {
  private final String name;
  private final String value;

  public XMLAttribute(String name, String value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    XMLAttribute that = (XMLAttribute) o;

    if (!name.equals(that.name)) return false;
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + value.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "XMLAttribute{" +
        "name='" + name + '\'' +
        ", value='" + value + '\'' +
        '}';
  }
}
