package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.model.Clazz;
import io.github.dumijdev.dpxml.model.Company;
import io.github.dumijdev.dpxml.model.Employee;
import io.github.dumijdev.dpxml.model.Person;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

public class DefaultPojolizerTests {

    private static String xml;
    private static String clazzXml;
    private static String employeeXml;
    private static String companyXml;
    private static Pojolizer pojolizer;

    @BeforeAll
    static void setup() {
        xml = "<root><name>Dumildes Paulo</name><age>77</age></root>";
        pojolizer = new DefaultPojolizer();
        clazzXml = "<root><person><name>Dumildes Paulo</name><age>77</age></person><person><name>Thiago Santana</name><age>77</age></person></root>";
        employeeXml = "<employee><id>5000</id><person><name>Dumildes Paulo</name><age>77</age></person></employee>";
        companyXml = "<root><id></id><employee><id>5000</id><person><name>Dumildes Paulo</name><age>77</age></person></employee></root>";
    }

    @DisplayName("Should Pojolize simple string")
    @Test
    @SneakyThrows
    void shouldPojolizeSimpleString() {

        var person = pojolizer.pojoify(xml, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Dumildes Paulo", person.getName());
        Assertions.assertEquals(77, person.getAge());
    }

    @DisplayName("Should returns an instance")
    @Test
    @SneakyThrows
    void shouldReturnsAnInstance() {

        var person = pojolizer.pojoify(xml, Person.class);

        Assertions.assertNotNull(person);
    }

    @DisplayName("Should returns a class with a list of person")
    @Test
    @SneakyThrows
    void shouldReturnsAClassWithAListOfPerson() {
        var clazz = pojolizer.pojoify(clazzXml, Clazz.class);

        Assertions.assertEquals(2, clazz.getPerson().size());
        Assertions.assertEquals("Dumildes Paulo", clazz.getPerson().get(0).getName());
        Assertions.assertEquals(77, clazz.getPerson().get(0).getAge());

    }

    @DisplayName("Should parse a complex object from xml string")
    @Test
    @SneakyThrows
    void shouldParseAComplexObjectFromXMLString() {
        var employee = pojolizer.pojoify(employeeXml, Employee.class);
        var person = pojolizer.pojoify(xml, Person.class);

        Assertions.assertNotNull(employee);
        Assertions.assertNotNull(employee.getPerson());
        Assertions.assertEquals("Dumildes Paulo", employee.getPerson().getName());
        Assertions.assertEquals(person, employee.getPerson());

    }

    @DisplayName("Should parse company object from xml string")
    @Test
    @SneakyThrows
    void shouldParseCompanyObjectFromXMLString() {
        var employee = pojolizer.pojoify(employeeXml, Employee.class);
        var company = pojolizer.pojoify(companyXml, Company.class);

        Assertions.assertNotNull(company);
        Assertions.assertEquals(1, company.getEmployees().size());
        Assertions.assertTrue(company.getEmployees().contains(employee));
        Assertions.assertEquals(employee, company.getEmployees().stream().findFirst().get());

    }

    @Test
    @SneakyThrows
    void shouldThrowsSAXParseException() {
        Assertions.assertThrows(SAXParseException.class, () -> pojolizer.pojoify("", Person.class));
    }

}
