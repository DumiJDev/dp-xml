package io.github.dumijdev.dpxml.utils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.time.temporal.Temporal;
import java.util.*;

public class ParserUtils {

    private static final Set<Class<?>> CONSIDERED_PRIMITIVES = new HashSet<>();

    static {
        CONSIDERED_PRIMITIVES.add(Integer.class);
        CONSIDERED_PRIMITIVES.add(Long.class);
        CONSIDERED_PRIMITIVES.add(Byte.class);
        CONSIDERED_PRIMITIVES.add(Short.class);
        CONSIDERED_PRIMITIVES.add(Float.class);
        CONSIDERED_PRIMITIVES.add(Double.class);
        CONSIDERED_PRIMITIVES.add(Boolean.class);
        CONSIDERED_PRIMITIVES.add(Character.class);

        CONSIDERED_PRIMITIVES.add(String.class);

        CONSIDERED_PRIMITIVES.add(Date.class);
        CONSIDERED_PRIMITIVES.add(java.sql.Date.class);
        CONSIDERED_PRIMITIVES.add(Temporal.class);

        CONSIDERED_PRIMITIVES.add(int.class);
        CONSIDERED_PRIMITIVES.add(long.class);
        CONSIDERED_PRIMITIVES.add(byte.class);
        CONSIDERED_PRIMITIVES.add(short.class);
        CONSIDERED_PRIMITIVES.add(float.class);
        CONSIDERED_PRIMITIVES.add(double.class);
        CONSIDERED_PRIMITIVES.add(boolean.class);
        CONSIDERED_PRIMITIVES.add(char.class);
    }

    public static String stringifyXml(Node node) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        DOMSource domSource = new DOMSource(node);

        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);

        transformer.transform(domSource, result);

        var xml = writer.toString();

        System.out.println("XML: " + xml);

        return xml;
    }

    public static Node findNode(Element parent, String name) {
         var nodes = findNodes(parent, name);

         return nodes.isEmpty() ? null : nodes.get(0);
    }

    public static List<Node> findNodes(Element parent, String name) {
        List<Node> out = new LinkedList<>();

        var children = parent.getChildNodes();
        for (var i = 0; i < children.getLength(); i++) {
            var item = children.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE && simpleNodeName(item.getNodeName()).equals(name)) {
                out.add(item);
            }
        }

        return out;
    }

    public static String simpleNodeName(String nodeName) {
        if (nodeName.contains(":")){
            return nodeName.split(":")[1];
        } else {
            return nodeName;
        }
    }

    public static boolean isCollection(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz);
    }

    public static boolean isList(Class<?> clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    public static boolean isSet(Class<?> clazz) {
        return Set.class.isAssignableFrom(clazz);
    }

    public static boolean isPrimitive(Class<?> clazz) {
        return CONSIDERED_PRIMITIVES.contains(clazz) || Temporal.class.isAssignableFrom(clazz);
    }
}
