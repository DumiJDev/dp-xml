package io.github.dumijdev.dpxml.exception;

public class UnXmlizableException extends RuntimeException {
  public UnXmlizableException() {
    super("Não é possível converter porque ele não é uma classe anotada com @Xmlizable.");
  }

  public UnXmlizableException(String clazz) {
    super(String.format("Não é possível converter porque %s" +
        " não é uma classe anotada com @Xmlizable.", clazz));
  }
}
