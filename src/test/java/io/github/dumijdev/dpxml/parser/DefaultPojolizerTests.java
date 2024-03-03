package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.model.Clazz;
import io.github.dumijdev.dpxml.model.Person;
import io.github.dumijdev.dpxml.model.Pojolizable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DefaultPojolizerTests {

    private static String xml;
    private static String clazzXml;
    private static Pojolizer pojolizer;

    @BeforeAll
    static void setup() {
        xml = "<root><name>Dumildes Paulo</name><age>77</age></root>";
        pojolizer = new DefaultPojolizer();
        clazzXml = "<root><person><name>Dumildes Paulo</name><age>77</age></person><person><name>Thiago Santana</name><age>77</age></person></root>";
    }

    @DisplayName("Should Pojolize simple string")
    @Test
    @SneakyThrows
    void shouldPojolizeSimpleString() {

        var person = pojolizer.convert(xml, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Dumildes Paulo", person.getName());
        Assertions.assertEquals(77, person.getAge());
    }

    @DisplayName("Should returns an instance")
    @Test
    @SneakyThrows
    void shouldReturnsAnInstance() {

        var person = pojolizer.convert(xml, Person.class);

        Assertions.assertNotNull(person);
    }

    @DisplayName("Should returns a class with a list of person")
    @Test
    @SneakyThrows
    void shouldReturnsAClassWithAListOfPerson() {
        var clazz = pojolizer.convert(clazzXml, Clazz.class);

        clazz.getPerson().forEach(person -> System.out.println(person.getName()));

        Assertions.assertEquals(2, clazz.getPerson().size());
        Assertions.assertEquals("Dumildes Paulo", clazz.getPerson().get(0).getName());
        Assertions.assertEquals(77, clazz.getPerson().get(0).getAge());
    }

}
