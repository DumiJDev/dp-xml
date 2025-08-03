package io.github.dumijdev.dpxml.parser.utils;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XPathUtils {
  private static final Map<String, XPathExpression> XPATH_CACHE = new ConcurrentHashMap<>();
  // Thread-local factories for thread safety
  private static final ThreadLocal<XPathFactory> XPATH_FACTORY =
      ThreadLocal.withInitial(XPathFactory::newInstance);

  public boolean matchesXPath(String path, String xpathExpr) {
    try {
      XPathExpression expr = getXPathExpression(xpathExpr);
      return path.matches(convertXPathToRegex(xpathExpr));
    } catch (Exception e) {
      return false;
    }
  }

  public XPathExpression getXPathExpression(String xpath) throws XPathExpressionException {
    return XPATH_CACHE.computeIfAbsent(xpath, key -> {
      try {
        XPath xPath = XPATH_FACTORY.get().newXPath();
        return xPath.compile(key);
      } catch (XPathExpressionException e) {
        throw new RuntimeException("Failed to compile XPath: " + key, e);
      }
    });
  }

  public String convertXPathToRegex(String xpath) {
    return xpath.replace("/", "\\/")
        .replace("*", ".*")
        .replace("//", ".*");
  }
}
