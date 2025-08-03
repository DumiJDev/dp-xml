package io.github.dumijdev.dpxml.deserializers;

import io.github.dumijdev.dpxml.parser.serializer.XmlDeserializer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateDeserializer implements XmlDeserializer<Date> {
  @Override
  public Date deserialize(String value) {
    var sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSSSS");

    try {
      return sdf.parse(value);
    } catch (ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
