package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.model.Company;
import io.github.dumijdev.dpxml.model.Person;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DefaultXmlizerTests {
    private static String xml;
    private static String clazzXml;
    private static String employeeXml;
    private static String companyXml;
    private static Pojolizer pojolizer;
    private static Xmlizer xmlizer;

    @BeforeAll
    static void setup() {
        xml = "<root><name>Dumildes Paulo</name><age>77</age></root>";
        pojolizer = new DefaultPojolizer();
        xmlizer = new DefaultXmlizer();
        clazzXml = "<root><person><name>Dumildes Paulo</name><age>77</age></person><person><name>Thiago Santana</name><age>77</age></person></root>";
        employeeXml = "<employee><id>5000</id><person><name>Dumildes Paulo</name><age>77</age></person></employee>";
        companyXml = "<root><id></id><employee><id>5000</id><person><name>Dumildes Paulo</name><age>77</age></person></employee></root>";
    }

    @DisplayName("Should xmlify")
    @Test
    @SneakyThrows
    void shouldXmlifyPOJO() {
        var pojo = pojolizer.pojoify(xml, Person.class);
        var pojoXml = xmlizer.xmlify(pojo);
        var pojo1 = pojolizer.pojoify(pojoXml, Person.class);

        System.out.println(pojoXml);

        Assertions.assertEquals(pojo, pojo1);
    }

    @DisplayName("Should xmlify a complex object")
    @Test
    @SneakyThrows
    void shouldXmlifyAComplexObject() {
        var pojo = pojolizer.pojoify(companyXml, Company.class);
        var pojoXml = xmlizer.xmlify(pojo);
        var pojo1 = pojolizer.pojoify(pojoXml, Company.class);

        System.out.println(pojoXml);

        Assertions.assertEquals(pojo, pojo1);
        Assertions.assertEquals(0, pojo1.getNames().size());
    }
}
