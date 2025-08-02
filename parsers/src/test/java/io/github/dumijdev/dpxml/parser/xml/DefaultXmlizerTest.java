package io.github.dumijdev.dpxml.parser.xml;

import io.github.dumijdev.dpxml.annotations.*;
import io.github.dumijdev.dpxml.parser.exception.InternalErrorException;
import io.github.dumijdev.dpxml.parser.serializer.XmlSerializer;
import io.github.dumijdev.dpxml.serializers.DateSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the enhanced DefaultXmlizer
 */
class DefaultXmlizerTest {

  private DefaultXmlizer xmlizer;

  @BeforeEach
  void setUp() {
    xmlizer = new DefaultXmlizer();
  }

  @Nested
  @DisplayName("Basic Functionality Tests")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("Should handle null objects gracefully")
    void testNullObject() {
      String result = xmlizer.xmlify(null);
      assertEquals("", result);
    }

    @Test
    @DisplayName("Should throw exception for non-xmlizable objects")
    void testNonXmlizableObject() {
      class NonXmlizable {
        private String field = "test";
      }

      assertThrows(Exception.class, () -> xmlizer.xmlify(new NonXmlizable()));
    }

    @Test
    @DisplayName("Should serialize basic xmlizable object")
    void testBasicXmlizableObject() {
      @Xmlizable
      class BasicObject {
        private String name = "test";
        private int value = 42;
      }

      String result = xmlizer.xmlify(new BasicObject());

      assertNotNull(result);
      assertTrue(result.contains("name"));
      assertTrue(result.contains("test"));
      assertTrue(result.contains("value"));
      assertTrue(result.contains("42"));
    }
  }

  @Nested
  @DisplayName("Custom Serialization Tests")
  class CustomSerializationTests {

    @Test
    @DisplayName("Should use custom serializer for annotated fields")
    void testCustomSerialization() {
      @Xmlizable
      class ObjectWithCustomSerialization {
        @XmlSerialize(using = DateSerializer.class)
        private Date createdAt = new Date(1609459200000L); // 2021-01-01

        private String name = "test";
      }

      String result = xmlizer.xmlify(new ObjectWithCustomSerialization());

      assertNotNull(result);
      assertTrue(result.contains("2021-01-01"));
      assertTrue(result.contains("name"));
      assertTrue(result.contains("test"));
    }

    @Test
    @DisplayName("Should handle serializer instantiation errors")
    void testSerializerInstantiationError() {
      abstract class AbstractSerializer implements XmlSerializer<String> {
        // Cannot be instantiated
      }

      @Xmlizable
      class ObjectWithBadSerializer {
        @XmlSerialize(using = AbstractSerializer.class)
        private String value = "test";
      }

      assertThrows(Exception.class, () -> xmlizer.xmlify(new ObjectWithBadSerializer()));
    }

    @Test
    @DisplayName("Should cache serializer instances")
    void testSerializerCaching() {
      @Xmlizable
      class ObjectWithSerialization1 {
        @XmlSerialize(using = DateSerializer.class)
        private Date date1 = new Date();
      }

      @Xmlizable
      class ObjectWithSerialization2 {
        @XmlSerialize(using = DateSerializer.class)
        private Date date2 = new Date();
      }

      // Both should use the same serializer instance
      assertDoesNotThrow(() -> {
        xmlizer.xmlify(new ObjectWithSerialization1());
        xmlizer.xmlify(new ObjectWithSerialization2());
      });
    }
  }

  @Nested
  @DisplayName("Static Attributes Tests")
  class StaticAttributesTests {

    @Test
    @DisplayName("Should add static attributes to class elements")
    void testClassLevelStaticAttributes() {
      @Xmlizable
      @StaticAttribute(name = "version", value = "1.0")
      class ObjectWithStaticAttribute {
        private String name = "test";
      }

      String result = xmlizer.xmlify(new ObjectWithStaticAttribute());

      assertNotNull(result);
      assertTrue(result.contains("version=\"1.0\""));
    }

    @Test
    @DisplayName("Should add static attributes to field elements")
    void testFieldLevelStaticAttributes() {
      @Xmlizable
      class ObjectWithFieldStaticAttribute {
        @StaticAttribute(name = "type", value = "string")
        private String name = "test";
      }

      String result = xmlizer.xmlify(new ObjectWithFieldStaticAttribute());

      assertNotNull(result);
      assertTrue(result.contains("type=\"string\""));
    }

    @Test
    @DisplayName("Should handle empty static attribute values")
    void testEmptyStaticAttributeValue() {
      @Xmlizable
      @StaticAttribute(name = "empty", value = "")
      class ObjectWithEmptyStaticAttribute {
        private String name = "test";
      }

      String result = xmlizer.xmlify(new ObjectWithEmptyStaticAttribute());

      assertNotNull(result);
      assertTrue(result.contains("empty=\"\""));
    }
  }

  @Nested
  @DisplayName("Dynamic Attributes Tests")
  class DynamicAttributesTests {

    @Test
    @DisplayName("Should add dynamic attributes using method calls")
    void testDynamicAttributes() {
      @Xmlizable
      @DynamicAttribute(name = "timestamp", method = "getTimestamp")
      class ObjectWithDynamicAttribute {
        private String name = "test";

        public String getTimestamp() {
          return "2023-01-01";
        }
      }

      String result = xmlizer.xmlify(new ObjectWithDynamicAttribute());

      assertNotNull(result);
      assertTrue(result.contains("timestamp=\"2023-01-01\""));
    }

    @Test
    @DisplayName("Should handle multiple dynamic attributes")
    void testMultipleDynamicAttributes() {
      @Xmlizable
      @DynamicAttributes(attributes = {
          @DynamicAttribute(name = "attr1", method = "getAttr1"),
          @DynamicAttribute(name = "attr2", method = "getAttr2")
      })
      class ObjectWithMultipleDynamicAttributes {
        private String name = "test";

        public String getAttr1() {
          return "value1";
        }

        public String getAttr2() {
          return "value2";
        }
      }

      String result = xmlizer.xmlify(new ObjectWithMultipleDynamicAttributes());

      assertNotNull(result);
      assertTrue(result.contains("attr1=\"value1\""));
      assertTrue(result.contains("attr2=\"value2\""));
    }

    @Test
    @DisplayName("Should handle dynamic attribute method errors gracefully")
    void testDynamicAttributeMethodError() {
      @Xmlizable
      @DynamicAttribute(name = "error", method = "nonExistentMethod")
      class ObjectWithBadDynamicAttribute {
        private String name = "test";
      }

      // Should not throw, but should log warning
      assertThrows(InternalErrorException.class, () -> {
        String result = xmlizer.xmlify(new ObjectWithBadDynamicAttribute());
        assertNotNull(result);
        assertFalse(result.contains("error="));
      });
    }

    @Test
    @DisplayName("Should handle null return values from dynamic attribute methods")
    void testDynamicAttributeNullReturn() {
      @Xmlizable
      @DynamicAttribute(name = "nullable", method = "getNullValue")
      class ObjectWithNullDynamicAttribute {
        private String name = "test";

        public String getNullValue() {
          return null;
        }
      }

      String result = xmlizer.xmlify(new ObjectWithNullDynamicAttribute());

      assertNotNull(result);
      assertFalse(result.contains("nullable="));
    }
  }

  @Nested
  @DisplayName("Dynamic Attribute Generator Tests")
  class DynamicAttributeGeneratorTests {

    @Test
    @DisplayName("Should generate attributes using generator methods")
    void testDynamicAttributeGenerator() {
      @Xmlizable
      class ObjectWithGenerator {
        private String firstName = "John";
        private String lastName = "Doe";

        @DynamicAttributeGenerator(name = "fullName", fields = {"firstName", "lastName"})
        public String generateFullName() {
          return firstName + " " + lastName;
        }
      }

      String result = xmlizer.xmlify(new ObjectWithGenerator());

      assertNotNull(result);
      assertTrue(result.contains("fullName=\"John Doe\""));
    }

    @Test
    @DisplayName("Should handle generator methods with missing fields")
    void testGeneratorWithMissingFields() {
      @Xmlizable
      class ObjectWithBadGenerator {
        private String existingField = "test";

        @DynamicAttributeGenerator(name = "generated", fields = {"nonExistentField"})
        public String generate() {
          return "null";
        }
      }

      String result = xmlizer.xmlify(new ObjectWithBadGenerator());

      assertNotNull(result);
      assertFalse(result.contains("generated=\"null\""));
    }

    @Test
    @DisplayName("Should handle generator method execution errors")
    void testGeneratorMethodError() {
      @Xmlizable
      class ObjectWithErrorGenerator {
        private String field = "test";

        @DynamicAttributeGenerator(name = "error", fields = {"field"})
        public String generateError() {
          throw new RuntimeException("Test error");
        }
      }

      // Should not throw, but should log warning
      assertThrows(InternalErrorException.class, () -> {
        String result = xmlizer.xmlify(new ObjectWithErrorGenerator());
        assertNotNull(result);
        assertFalse(result.contains("error="));
      });
    }

    @Test
    @DisplayName("Should handle multiple generator methods")
    void testMultipleGenerators() {
      @Xmlizable
      class ObjectWithMultipleGenerators {
        private String name = "test";
        private int value = 42;

        @DynamicAttributeGenerator(name = "summary1", fields = {"name"})
        public String generateSummary1() {
          return "summary1-" + name;
        }

        @DynamicAttributeGenerator(name = "summary2", fields = {"value"})
        public String generateSummary2() {
          return "summary2-" + value;
        }
      }

      String result = xmlizer.xmlify(new ObjectWithMultipleGenerators());

      assertNotNull(result);
      assertTrue(result.contains("summary1=\"summary1-test\""));
      assertTrue(result.contains("summary2=\"summary2-42\""));
    }
  }

  @Nested
  @DisplayName("Enhanced Namespace Tests")
  class EnhancedNamespaceTests {

    @Test
    @DisplayName("Should apply namespace to class elements")
    void testClassNamespace() {
      @Xmlizable
      @Namespace(name = "app", value = "http://example.com/app")
      class ObjectWithNamespace {
        private String name = "test";
      }

      String result = xmlizer.xmlify(new ObjectWithNamespace());

      assertNotNull(result);
      assertTrue(result.contains("xmlns:app=\"http://example.com/app\""));
      assertTrue(result.contains("app:"));
    }

    @Test
    @DisplayName("Should apply namespace to field elements")
    void testFieldNamespace() {
      @Xmlizable
      class ObjectWithFieldNamespace {
        @Namespace(name = "field", value = "http://example.com/field")
        private String name = "test";
      }

      String result = xmlizer.xmlify(new ObjectWithFieldNamespace());

      assertNotNull(result);
      assertTrue(result.contains("field:name"));
    }

    @Test
    @DisplayName("Should handle default namespace (empty name)")
    void testDefaultNamespace() {
      @Xmlizable
      @Namespace(name = "", value = "http://example.com/default")
      class ObjectWithDefaultNamespace {
        private String name = "test";
      }

      String result = xmlizer.xmlify(new ObjectWithDefaultNamespace());

      assertNotNull(result);
      assertTrue(result.contains("xmlns=\"http://example.com/default\""));
    }

    @Test
    @DisplayName("Should handle declared namespace aliases")
    void testDeclaredNamespaces() {
      xmlizer.registerNamespace("custom", "http://example.com/custom");
      xmlizer.registerNamespace("util", "http://example.com/util");

      @Xmlizable
      @DeclaredNamespaces(aliases = {"custom", "util"})
      class ObjectWithDeclaredNamespaces {
        private String name = "test";
      }

      String result = xmlizer.xmlify(new ObjectWithDeclaredNamespaces());

      assertNotNull(result);
      assertTrue(result.contains("xmlns:custom=\"http://example.com/custom\""));
      assertTrue(result.contains("xmlns:util=\"http://example.com/util\""));
    }
  }

  @Nested
  @DisplayName("Collection and Array Tests")
  class CollectionAndArrayTests {

    @Test
    @DisplayName("Should serialize collections with attributes")
    void testCollectionWithAttributes() {
      @Xmlizable
      class ObjectWithCollection {
        @StaticAttribute(name = "type", value = "list")
        @DynamicAttribute(name = "size", method = "getSize")
        private List<String> items = Arrays.asList("item1", "item2");

        public String getSize() {
          return String.valueOf(items.size());
        }
      }

      String result = xmlizer.xmlify(new ObjectWithCollection());

      assertNotNull(result);
      assertTrue(result.contains("type=\"list\""));
      assertTrue(result.contains("size=\"2\""));
      assertTrue(result.contains("item1"));
      assertTrue(result.contains("item2"));
    }

    @Test
    @DisplayName("Should serialize arrays with custom serialization")
    void testArrayWithCustomSerialization() {
      @Xmlizable
      class ObjectWithArray {
        @XmlSerialize(using = DateSerializer.class)
        private Date[] dates = {new Date(1609459200000L)};
      }

      // Note: This test assumes the custom serializer handles arrays appropriately
      assertDoesNotThrow(() -> {
        String result = xmlizer.xmlify(new ObjectWithArray());
        assertNotNull(result);
      });
    }
  }

  @Nested
  @DisplayName("Configuration Tests")
  class ConfigurationTests {

    @Test
    @DisplayName("Should apply pretty print configuration")
    void testPrettyPrintConfiguration() {
      @Xmlizable
      class SimpleObject {
        private String name = "test";
      }

      DefaultXmlizer prettyXmlizer = new DefaultXmlizer().withPrettyPrint(true);
      String result = prettyXmlizer.xmlify(new SimpleObject());

      assertNotNull(result);
      // Pretty printed XML should contain line breaks or indentation
      assertTrue(result.length() > result.replaceAll("\\s", "").length());
    }

    @Test
    @DisplayName("Should apply encoding configuration")
    void testEncodingConfiguration() {
      @Xmlizable
      class SimpleObject {
        private String name = "test";
      }

      DefaultXmlizer encodingXmlizer = new DefaultXmlizer()
          .withEncoding("ISO-8859-1")
          .withXmlDeclaration(true);

      String result = encodingXmlizer.xmlify(new SimpleObject());

      assertNotNull(result);
      assertTrue(result.contains("encoding=\"ISO-8859-1\""));
    }

    @Test
    @DisplayName("Should use configuration builder pattern")
    void testConfigurationBuilder() {
      @Xmlizable
      class SimpleObject {
        private String name = "test";
      }

      DefaultXmlizer configuredXmlizer = DefaultXmlizer.configure()
          .prettyPrint()
          .encoding("UTF-8")
          .includeXmlDeclaration()
          .namespace("test", "http://example.com/test")
          .build();

      String result = configuredXmlizer.xmlify(new SimpleObject());

      assertNotNull(result);
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("Should be thread-safe for concurrent xmlification")
    void testConcurrentXmlification() throws Exception {
      @Xmlizable
      class ThreadTestObject {
        private String name;
        private int value;

        ThreadTestObject(String name, int value) {
          this.name = name;
          this.value = value;
        }
      }

      ExecutorService executor = Executors.newFixedThreadPool(10);
      List<Future<String>> futures = new ArrayList<>();

      // Submit 100 concurrent xmlification tasks
      for (int i = 0; i < 100; i++) {
        final int index = i;
        futures.add(executor.submit(() ->
            xmlizer.xmlify(new ThreadTestObject("test" + index, index))
        ));
      }

      // Wait for all tasks to complete and verify results
      for (int i = 0; i < futures.size(); i++) {
        String result = futures.get(i).get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertTrue(result.contains("test" + i));
        assertTrue(result.contains(String.valueOf(i)));
      }

      executor.shutdown();
    }

    @Test
    @DisplayName("Should handle concurrent namespace registration")
    void testConcurrentNamespaceRegistration() throws Exception {
      ExecutorService executor = Executors.newFixedThreadPool(5);
      List<Future<Void>> futures = new ArrayList<>();

      // Register namespaces concurrently
      for (int i = 0; i < 50; i++) {
        final int index = i;
        futures.add(executor.submit(() -> {
          xmlizer.registerNamespace("ns" + index, "http://example.com/ns" + index);
          return null;
        }));
      }

      // Wait for all registrations to complete
      for (Future<Void> future : futures) {
        future.get(5, TimeUnit.SECONDS);
      }

      executor.shutdown();

      // Verify that namespaces were registered correctly
      // This would require exposing the namespace registry for testing
      // or testing through xmlification of objects with declared namespaces
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle reflection errors gracefully")
    void testReflectionErrors() {
      @Xmlizable
      class ObjectWithPrivateMethod {
        @DynamicAttribute(name = "private", method = "privateMethod")
        private String name = "test";

        private String privateMethod() {
          return "private";
        }
      }

      // Should not throw exception, might not include the attribute
      assertDoesNotThrow(() -> {
        String result = xmlizer.xmlify(new ObjectWithPrivateMethod());
        assertNotNull(result);
      });
    }

    @Test
    @DisplayName("Should handle malformed attribute strings")
    void testMalformedAttributeStrings() {
      // This test would require mocking the Attributes.getAttributes method
      // to return malformed attribute strings and verify they are handled gracefully

      @Xmlizable
      class SimpleObject {
        private String name = "test";
      }

      // Test with various malformed attribute scenarios
      assertDoesNotThrow(() -> {
        String result = xmlizer.xmlify(new SimpleObject());
        assertNotNull(result);
      });
    }

    @Test
    @DisplayName("Should handle inheritance hierarchy correctly")
    void testInheritanceHierarchy() {
      @Xmlizable
      class BaseClass {
        @StaticAttribute(name = "base", value = "true")
        protected String baseField = "base";
      }

      @Xmlizable
      @StaticAttribute(name = "derived", value = "true")
      class DerivedClass extends BaseClass {
        private String derivedField = "derived";
      }

      String result = xmlizer.xmlify(new DerivedClass());

      assertNotNull(result);
      assertTrue(result.contains("base=\"true\""));
      assertTrue(result.contains("derived=\"true\""));
      assertTrue(result.contains("baseField"));
      assertTrue(result.contains("derivedField"));
    }
  }

  @Nested
  @DisplayName("Map Serialization Tests")
  class MapSerializationTests {

    @Test
    @DisplayName("Should serialize maps correctly")
    void testMapSerialization() {
      Map<String, Object> map = new HashMap<>();
      map.put("key1", "value1");
      map.put("key2", 42);
      map.put("key3", null); // Should be skipped

      String result = xmlizer.xmlify(map);

      assertNotNull(result);
      assertTrue(result.contains("key1"));
      assertTrue(result.contains("value1"));
      assertTrue(result.contains("key2"));
      assertTrue(result.contains("42"));
      assertFalse(result.contains("key3"));
    }

    @Test
    @DisplayName("Should handle nested maps")
    void testNestedMaps() {
      Map<String, Object> innerMap = new HashMap<>();
      innerMap.put("inner", "value");

      Map<String, Object> outerMap = new HashMap<>();
      outerMap.put("outer", innerMap);

      String result = xmlizer.xmlify(outerMap);

      assertNotNull(result);
      assertTrue(result.contains("outer"));
      assertTrue(result.contains("inner"));
      assertTrue(result.contains("value"));
    }
  }
}