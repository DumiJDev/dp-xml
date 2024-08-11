# DP-XML 

## Descrição

O projeto **dp-xml** é uma biblioteca Java que permite a conversão de dados no formato xml para **POJO**(Plain Old Java
Object) e vice-versa

## Pré-requisitos

Para usar esta biblioteca, você precisará ter o seguinte instalado em seu sistema:

- Java 11 ou superior
- Maven

## Instalação

Para instalar a biblioteca em seu projeto Maven, adicione a seguinte dependência ao seu arquivo `pom.xml`:

```xml

<dependency>
    <groupId>io.github.dumijdev</groupId>
    <artifactId>dp-xml</artifactId>
    <version>0.2.0</version>
</dependency>
```

## Uso

Aqui está um exemplo de como você pode usar a biblioteca para converter **POJO** para xml e vice versa:

```java

import io.github.dumijdev.dpxml.parser.impl.pojo.BasicPojolizer;
import io.github.dumijdev.dpxml.parser.impl.pojo.FlexBasicPojolizer;
import io.github.dumijdev.dpxml.stereotype.*;
import io.github.dumijdev.dpxml.parser.impl.xml.DefaultXmlizer;

import java.util.UUID;

@Pojolizable //Able a class be convert to pojo
@Xmlizable //Able a class be convert to xml
class Student {
    @Element(name = "name") //Optional annotation
    @StaticAttribute(name = "title", value = "test")
    @DynamicAttribute(name = "dynamicTitle", method = "generateTitle")
    private String name;

    public Student() {
        this.name = "Test";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String generateTitle() {
        return UUID.randomUUID().toString();
    }
}

class SimpleStudent {
    @FlexElement(src = "data.students.student.name", dst = "name")
    private String name;

    public SimpleStudent() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

Xmlizer xmlizer = new DefaultXmlizer();
var xmlStudent = xmlizer.xmlify(new Student());

Pojolizer pojolizer = new BasicPojolizer();
var student = pojolizer.pojoify(xmlStudent, Student.class);

var xmlSimple = "<root><data><students><student><name>Dumildes Paulo</name></student></students></data></root>";

var flexPojolizer = new FlexBasicPojolizer();
var simpleStudent = flexPojolizer.pojoify(xmlSimple, SimpleStudent.class);

```

## Contribuição

Contribuições para o projeto são bem-vindas. Por favor, leia as diretrizes de contribuição antes de enviar um pull
request.

## Licença

Este projeto está licenciado sob a licença MIT. Consulte o arquivo `LICENSE` para obter detalhes.