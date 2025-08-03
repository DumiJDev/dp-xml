package io.github.dumijdev.dpxml.sample;

import io.github.dumijdev.dpxml.annotations.Element;
import io.github.dumijdev.dpxml.annotations.Pojolizable;
import io.github.dumijdev.dpxml.annotations.Xmlizable;
import io.github.dumijdev.dpxml.parser.DPXMLMapper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Sample {
  public static void main(String[] args) {
    @Xmlizable
    @Pojolizable
    class SampleClass {
      @Element(name = "name")
      private final String name;
      private final Date today;

      SampleClass(@Element(name = "name") String name, @Element(name = "today") Date today) {
        this.name = name;
        this.today = today;
      }

      @Override
      public String toString() {
        return "SampleClass{" +
            "name='" + name + '\'' +
            ", today=" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSSSS").format(today) +
            '}';
      }
    }

    var sampleClass = new SampleClass("Arg0", new Date());

    DPXMLMapper dpxmlMapper = new DPXMLMapper();
    var xml = dpxmlMapper.toXML(sampleClass);
    System.out.println(xml);
    System.out.println(dpxmlMapper.toPojo(xml, SampleClass.class));
  }
}
