package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.parser.impl.node.DefaultNodilizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NodilizerTest {
  private String wsdl;

  @BeforeEach
  void setUp() {
    try {
      var path = NodilizerTest.class.getResource("/service.wsdl").getPath();
      System.out.println(path);
      wsdl = Files.readString(Path.of(path.replaceAll("/C:", "C:")));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void nodify() {
    Nodilizer nodilizer = new DefaultNodilizer();

    var node = nodilizer.nodify(wsdl);

    Assertions.assertEquals(8, node.child("types").child("schema").children("element").size());
  }
}