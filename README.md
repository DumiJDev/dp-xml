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
    <version>0.0.1</version>
</dependency>
```

## Uso

Aqui está um exemplo de como você pode usar a biblioteca para converter **POJO** para xml e vice versa:

```java

import io.github.dumijdev.dpxml.model.Pojolizable;
import io.github.dumijdev.dpxml.model.Xmlizable;
import io.github.dumijdev.dpxml.parser.impl.DefaultPojolizer;
import io.github.dumijdev.dpxml.parser.impl.DefaultXmlizer;
import io.github.dumijdev.dpxml.stereotype.Element;

@Pojolizable //Able a class be convert to pojo
@Xmlizable //Able a class be convert to xml
class Student {
    @Element(name = "name") //Optional annotation
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
}

    Xmlizer xmlizer = new DefaultXmlizer();
    var xmlStudent = xmlizer.xmlify(new Student());

    Pojolizer pojolizer = new DefaultPojolizer();
    var student = pojolizer.pojoify(xmlStudent, Student.class);

```

## Contribuição

Contribuições para o projeto são bem-vindas. Por favor, leia as diretrizes de contribuição antes de enviar um pull
request.

## Licença

Este projeto está licenciado sob a licença MIT. Consulte o arquivo `LICENSE` para obter detalhes.