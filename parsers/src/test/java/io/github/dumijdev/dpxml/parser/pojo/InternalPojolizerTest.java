package io.github.dumijdev.dpxml.parser.pojo;

import io.github.dumijdev.dpxml.annotations.Element;
import io.github.dumijdev.dpxml.annotations.FlexElement;
import io.github.dumijdev.dpxml.annotations.IgnoreElement;
import io.github.dumijdev.dpxml.annotations.XmlDeserialize;
import io.github.dumijdev.dpxml.parser.node.DefaultNodilizer;
import io.github.dumijdev.dpxml.parser.serializer.XmlDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InternalPojolizerTest {

  private InternalPojolizer pojolizer;

  @BeforeEach
  void setUp() {
    pojolizer = new InternalPojolizer();
  }

  // Test simple types
  @Test
  void testSimpleTypes() {
    String xml =
        "<SimpleTypes>" +
            "<stringValue>test</stringValue>" +
            "<intValue>42</intValue>" +
            "<booleanValue>true</booleanValue>" +
            "<doubleValue>3.14</doubleValue>" +
            "</SimpleTypes>";

    SimpleTypes result = pojolizer.pojoify(xml, SimpleTypes.class);

    assertEquals("test", result.stringValue);
    assertEquals(42, result.intValue);
    assertTrue(result.booleanValue);
    assertEquals(3.14, result.doubleValue);
  }

  // Test simple types
  @Test
  void testSimpleTypesWithInterface() {
    String xml =
        "<SimpleTypes>" +
            "<stringValue>test</stringValue>" +
            "<intValue>42</intValue>" +
            "<booleanValue>true</booleanValue>" +
            "<doubleValue>3.14</doubleValue>" +
            "</SimpleTypes>";

    var result = pojolizer.pojoify(xml, SimpleTypesInterface.class);

    assertEquals("test", result.stringValue());
    assertEquals(42, result.intValue());
    assertTrue(result.booleanValue());
    assertEquals(3.14, result.doubleValue());
  }

  // Test arrays
  @Test
  void testArrays() {
    String xml =
        "<ArrayContainer>" +
            "<numbers>1 2 3 4 5</numbers>" +
            "<strings>hello world test</strings>" +
            "</ArrayContainer>";

    ArrayContainer result = pojolizer.pojoify(xml, ArrayContainer.class);

    assertArrayEquals(new int[]{1, 2, 3, 4, 5}, result.numbers);
    assertArrayEquals(new String[]{"hello", "world", "test"}, result.strings);
  }

  // Test collections
  @Test
  void testCollections() {
    String xml =
        "<CollectionContainer>" +
            "<numberList>1 2 3</numberList>" +
            "<stringSet>a b c</stringSet>" +
            "</CollectionContainer>";

    CollectionContainer result = pojolizer.pojoify(xml, CollectionContainer.class);

    assertEquals(List.of(1, 2, 3), result.numberList);
    assertEquals(Set.of("a", "b", "c"), result.stringSet);
  }

  // Test nested objects
  @Test
  void testNestedObjects() {
    String xml =
        "<Person>" +
            "<name>John Doe</name>" +
            "<address>" +
            "<street>123 Main St</street>" +
            "<city>Springfield</city>" +
            "</address>" +
            "</Person>";

    Person result = pojolizer.pojoify(xml, Person.class);

    assertEquals("John Doe", result.name);
    assertNotNull(result.address);
    assertEquals("123 Main St", result.address.street);
    assertEquals("Springfield", result.address.city);
  }

  // Test custom deserializer
  @Test
  void testCustomDeserializer() {
    String xml = "<CustomData>2024-03-20</CustomData>";

    CustomData result = pojolizer.pojoify(xml, CustomData.class);

    assertEquals("2024-03-20", result.value);
  }

  // Test annotations
  @Test
  void testAnnotations() {
    String xml =
        "<AnnotatedClass>" +
            "<customName>Test</customName>" +
            "<ignoredField>Should not be set</ignoredField>" +
            "<notAnnotated>" +
            "<flexField>Flex value</flexField>" +
            "</notAnnotated>" +
            "</AnnotatedClass>";

    AnnotatedClass result = pojolizer.pojoify(xml, AnnotatedClass.class);

    assertEquals("Test", result.renamedField);
    assertNull(result.ignoredField);
    assertEquals("Flex value", result.notAnnotated.flexField);
  }

  // Test failure cases
  @Test
  void testInvalidXml() {
    String invalidXml = "<InvalidXml><unclosed>";
    assertThrows(RuntimeException.class, () ->
        pojolizer.pojoify(invalidXml, SimpleTypes.class)
    );
  }

  @Test
  void testInvalidNumberFormat() {
    String xml = "<SimpleTypes><intValue>not a number</intValue></SimpleTypes>";
    assertThrows(RuntimeException.class, () ->
        pojolizer.pojoify(xml, SimpleTypes.class)
    );
  }

  // Test empty or null values
  @Test
  void testEmptyValues() {
    String xml =
        "<SimpleTypes>" +
            "<stringValue></stringValue>" +
            "<intValue>0</intValue>" +
            "</SimpleTypes>";

    SimpleTypes result = pojolizer.pojoify(xml, SimpleTypes.class);

    assertEquals(null, result.stringValue);
    assertEquals(0, result.intValue);
  }

  // Test classes
  static class SimpleTypes {
    private String stringValue;
    private int intValue;
    private boolean booleanValue;
    private double doubleValue;
  }

  interface SimpleTypesInterface {
    String stringValue();
    int intValue();
    boolean booleanValue();
    double doubleValue();
  }

  static class ArrayContainer {
    private int[] numbers;
    private String[] strings;
  }

  static class CollectionContainer {
    private List<Integer> numberList;
    private Set<String> stringSet;
  }

  static class Person {
    private String name;
    private Address address;
  }

  static class Address {
    private String street;
    private String city;
  }

  @XmlDeserialize(using = CustomDataDeserializer.class)
  static class CustomData {
    private String value;
  }

  static class CustomDataDeserializer implements XmlDeserializer<CustomData> {
    @Override
    public CustomData deserialize(String xml) {
      CustomData data = new CustomData();
      data.value = new DefaultNodilizer().nodify(xml).content();
      return data;
    }
  }

  static class AnnotatedClass {
    @Element(name = "customName")
    private String renamedField;

    @IgnoreElement
    private String ignoredField;

    private TestClass notAnnotated;

  }

  static class TestClass {
    @FlexElement(src = "AnnotatedClass.notAnnotated.flexField")
    private String flexField;
  }

}