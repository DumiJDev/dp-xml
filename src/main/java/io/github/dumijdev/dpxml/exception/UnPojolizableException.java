package io.github.dumijdev.dpxml.exception;

public class UnPojolizableException extends RuntimeException {
  public UnPojolizableException() {
    super("Não é possível converter porque ele não é uma classe anotada com Pojolize.");
  }

  public UnPojolizableException(String clazz) {
    super(String.format("Não é possível converter porque %s" +
        " não é uma classe anotada com Pojolize.", clazz));
  }
}
