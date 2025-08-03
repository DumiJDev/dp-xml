package io.github.dumijdev.dpxml.serializers;

import io.github.dumijdev.dpxml.parser.serializer.XmlSerializer;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateSerializer implements XmlSerializer<Date> {
  @Override
  public String serialize(Date date) {
    return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSSSS").format(date);
  }
}
