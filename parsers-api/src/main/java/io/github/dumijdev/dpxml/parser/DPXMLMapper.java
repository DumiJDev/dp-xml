package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.parser.api.AbstractNodilizer;
import io.github.dumijdev.dpxml.parser.api.AbstractPojolizer;
import io.github.dumijdev.dpxml.parser.api.AbstractXmlizer;
import io.github.dumijdev.dpxml.parser.factories.NodilizerFactory;
import io.github.dumijdev.dpxml.parser.factories.PojolizerFactory;
import io.github.dumijdev.dpxml.parser.factories.XmlizerFactory;
import io.github.dumijdev.dpxml.parser.serializer.XmlDeserializer;
import io.github.dumijdev.dpxml.parser.serializer.XmlSerializer;

import java.util.Objects;

public class DPXMLMapper {
  private final AbstractPojolizer pojolizer;
  private final AbstractXmlizer xmlizer;

  public DPXMLMapper() {
    this.pojolizer = Objects.requireNonNull(PojolizerFactory.find(), "Cannot found an implementation of Pojolizer.");
    this.xmlizer = Objects.requireNonNull(XmlizerFactory.find(), "Cannot found an implementation of Xmlizer.");
  }

  public DPXMLMapper(DPXMLMapper mapper) {
    this.pojolizer = mapper.pojolizer;
    this.xmlizer = mapper.xmlizer;
  }

  public <T> T toPojo(String xml, Class<T> clazz) {
    return pojolizer.pojoify(xml, clazz);
  }

  public String toXML(Object object) {
    return xmlizer.xmlify(object);
  }

  public DPXMLMapper copy() {
    return new DPXMLMapper(this);
  }

  public <T> DPXMLMapper addSerializer(Class<T> clazz, XmlSerializer<T> xmlSerializer) {
    xmlizer.addSerializer(clazz, xmlSerializer);
    return this;
  }

  public <T> DPXMLMapper addDeserializer(Class<T> clazz, XmlDeserializer<T> xmlDeserializer) {
    pojolizer.addDeserializer(clazz, xmlDeserializer);
    return this;
  }
}
