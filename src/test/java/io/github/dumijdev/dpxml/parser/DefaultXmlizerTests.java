package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.model.Company;
import io.github.dumijdev.dpxml.model.Employee;
import io.github.dumijdev.dpxml.model.MapTest;
import io.github.dumijdev.dpxml.model.Person;
import io.github.dumijdev.dpxml.parser.impl.pojo.BasicPojolizer;
import io.github.dumijdev.dpxml.parser.impl.xml.DefaultXmlizer;
import org.junit.jupiter.api.*;

class DefaultXmlizerTests {
  private static String xml;
  private static String clazzXml;
  private static String employeeXml;
  private static String companyXml;
  private static Pojolizer pojolizer;
  private static Xmlizer xmlizer;

  @BeforeAll
  static void setup() throws Exception {
    xml = "<root><name>Dumildes Paulo</name><age>77</age></root>";
    pojolizer = new BasicPojolizer();
    xmlizer = new DefaultXmlizer().registerNamespace("ns3", "https://1234.tes").registerNamespace("ns4", "https://1234.tes");
    clazzXml = "<root><person><name>Dumildes Paulo</name><age>77</age></person><person><name>Thiago Santana</name><age>77</age></person></root>";
    employeeXml = "<employee><id>5000</id><person><name>Dumildes Paulo</name><age>77</age></person></employee>";
    companyXml = "<root><id></id><employees><employee><id>5000</id><person><name>Dumildes Paulo</name><age>77</age></person></employee><employee><id>5000</id><person><name>Dumildes Paulo</name><age>77</age></person></employee></employees></root>";
  }

  @DisplayName("Should xmlify")
  @Test
  void shouldXmlifyPOJO() {
    var pojo = pojolizer.pojoify(xml, Person.class);
    var pojoXml = xmlizer.xmlify(pojo);
    var pojo1 = pojolizer.pojoify(pojoXml, Person.class);

    System.out.println(pojoXml);

    Assertions.assertEquals(pojo, pojo1);
  }

  @DisplayName("Should xmlify a complex object")
  @Test
  void shouldXmlifyAComplexObject() {
    var pojo = pojolizer.pojoify(companyXml, Company.class);
    var pojoXml = xmlizer.xmlify(pojo);
    var pojo1 = pojolizer.pojoify(pojoXml, Company.class);

    System.out.println(pojoXml);

    Assertions.assertEquals(pojo.getId(), pojo1.getId());
    Assertions.assertEquals(0, pojo1.getNames().size());
    Assertions.assertNotNull(pojo1.getEmployees());
  }

  @Test
  void shouldParseComplexObjectWithAnotherObject() {
    var pojo = pojolizer.pojoify(employeeXml, Employee.class);
    var pojoXml = xmlizer.xmlify(pojo);

    System.out.println(pojoXml);

    Assertions.assertNotNull(pojo);
    Assertions.assertFalse(pojoXml.isEmpty());
  }

  @Test
  void shouldParseClassWithMap() {
    var pojo = new MapTest();

    var pojoXml = xmlizer.xmlify(pojo);

    System.out.println(pojoXml);

    Assertions.assertNotNull(pojo);
    Assertions.assertFalse(pojoXml.isEmpty());
  }

}
