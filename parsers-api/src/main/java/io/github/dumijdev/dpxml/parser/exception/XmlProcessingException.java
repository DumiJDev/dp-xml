package io.github.dumijdev.dpxml.parser.exception;

/**
 * Enhanced exception for XML processing errors
 */
public class XmlProcessingException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final String xmlContext;

  public XmlProcessingException(String message) {
    this(message, null);
  }

  public XmlProcessingException(String message, Throwable cause) {
    this(message, cause, null);
  }

  public XmlProcessingException(String message, Throwable cause, String xmlContext) {
    super(message + (xmlContext != null ? " (Context: " + xmlContext + ")" : ""), cause);
    this.xmlContext = xmlContext;
  }

  public String getXmlContext() {
    return xmlContext;
  }
}
