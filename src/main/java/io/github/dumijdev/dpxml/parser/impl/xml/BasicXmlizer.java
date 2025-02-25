package io.github.dumijdev.dpxml.parser.impl.xml;

import io.github.dumijdev.dpxml.exception.InternalErrorException;
import io.github.dumijdev.dpxml.exception.UnXmlizableException;
import io.github.dumijdev.dpxml.model.Node;
import io.github.dumijdev.dpxml.model.XMLNode;
import io.github.dumijdev.dpxml.parser.Xmlizer;
import io.github.dumijdev.dpxml.stereotype.*;
import io.github.dumijdev.dpxml.utils.Attributes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static io.github.dumijdev.dpxml.utils.ParserUtils.*;

/**
 * Implementação básica de Xmlizer que converte objetos para Node
 * antes de serializá-los para XML.
 */
public class BasicXmlizer implements Xmlizer {
    private final Map<String, String> namespaces = new HashMap<>();

    @Override
    public String xmlify(Object obj) {
        if (obj == null) {
            return "";
        }

        // Se já for um Node, usa diretamente
        if (obj instanceof Node) {
            return xmlify((Node) obj);
        }

        // Converte o objeto para Node e depois serializa
        try {
            Node node = objectToNode(obj, null, null);
            return node.asXml();
        } catch (Exception e) {
            throw new InternalErrorException(e);
        }
    }

    @Override
    public String xmlify(Node node) {
        return node != null ? node.asXml() : "";
    }

    /**
     * Converte um objeto para uma representação Node.
     */
    private Node objectToNode(Object obj, String name, String namespace)
        throws IllegalAccessException, InvocationTargetException {
        if (obj == null) {
            return new XMLNode();
        }

        Class<?> clazz = obj.getClass();

        // Verifica se o objeto pode ser serializado para XML
        if (!clazz.isAnnotationPresent(Xmlizable.class) && !isMap(clazz)) {
            throw new UnXmlizableException(clazz.getSimpleName());
        }

        // Trata mapas de forma especial
        if (isMap(clazz)) {
            return mapToNode((Map<?, ?>) obj, namespace);
        }

        // Determina o nome do nó raiz
        String rootName = determineNodeName(name, namespace, clazz);
        String nsPrefix = extractNamespacePrefix(rootName);
        String localName = extractLocalName(rootName);

        // Cria o nó raiz
        XMLNode rootNode = new XMLNode(localName, nsPrefix);

        // Adiciona atributos do objeto
        addAttributesToNode(rootNode, clazz, obj);

        // Adiciona namespaces declarados
        addNamespacesToNode(rootNode, clazz);

        // Processa os campos do objeto
        processObjectFields(obj, clazz, rootNode);

        return rootNode;
    }

    /**
     * Processa todos os campos do objeto e adiciona ao nó.
     */
    private void processObjectFields(Object obj, Class<?> clazz, XMLNode parentNode)
        throws IllegalAccessException, InvocationTargetException {
        for (Field field : clazz.getDeclaredFields()) {
            // Ignora campos marcados para serem ignorados
            if (field.isAnnotationPresent(IgnoreElement.class)) {
                continue;
            }

            field.setAccessible(true);
            Object fieldValue = field.get(obj);

            if (fieldValue == null) {
                continue;
            }

            // Determina o nome do elemento baseado nas anotações
            String fieldElementName = determineFieldName(field);
            String nsPrefix = extractNamespacePrefix(fieldElementName);
            String localName = extractLocalName(fieldElementName);

            // Processa campos baseado no tipo
            if (isCollection(field.getType())) {
                processCollectionField(field, localName, nsPrefix, (Collection<?>) fieldValue, parentNode);
            } else {
                processSingleField(field, localName, nsPrefix, fieldValue, parentNode);
            }
        }
    }

    /**
     * Processa um campo que contém uma coleção.
     */
    private void processCollectionField(Field field, String localName, String nsPrefix, Collection<?> values, XMLNode parentNode)
        throws IllegalAccessException, InvocationTargetException {
        for (Object element : values) {
            if (element == null) {
                continue;
            }

            if (isPrimitive(element.getClass())) {
                // Para valores primitivos, cria um nó com o valor como conteúdo
                XMLNode childNode = new XMLNode(localName, nsPrefix);
                childNode.setContent(element.toString());
                parentNode.addChild(localName, childNode);
            } else {
                // Para objetos complexos, converte recursivamente para nó
                String namespace = getNamespaceFromField(field);
                Node childNode = objectToNode(element, localName, namespace);
                parentNode.addChild(localName, childNode);
            }
        }
    }

    /**
     * Processa um campo que contém um único valor.
     */
    private void processSingleField(Field field, String localName, String nsPrefix, Object fieldValue, XMLNode parentNode)
        throws IllegalAccessException, InvocationTargetException {
        if (isPrimitive(field.getType())) {
            // Para tipos primitivos, cria um nó com o valor como conteúdo
            XMLNode childNode = new XMLNode(localName, nsPrefix);
            childNode.setContent(fieldValue.toString());
            parentNode.addChild(localName, childNode);
        } else {
            // Para objetos complexos, converte recursivamente para nó
            String namespace = getNamespaceFromField(field);
            Node childNode = objectToNode(fieldValue, localName, namespace);
            parentNode.addChild(localName, childNode);
        }
    }

    /**
     * Converte um Map para um Node.
     */
    private Node mapToNode(Map<?, ?> map, String namespace)
        throws IllegalAccessException, InvocationTargetException {
        XMLNode rootNode = new XMLNode();
        String nsPrefix = namespace != null ? namespace : "";

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (value == null || !(key instanceof String)) {
                continue;
            }

            String keyStr = (String) key;

            if (isPrimitive(value.getClass())) {
                // Para valores primitivos, cria um nó com o valor como conteúdo
                XMLNode childNode = new XMLNode(keyStr, nsPrefix);
                childNode.setContent(value.toString());
                rootNode.addChild(keyStr, childNode);
            } else {
                // Para objetos complexos, converte recursivamente para nó
                Node childNode = objectToNode(value, keyStr, nsPrefix);
                rootNode.addChild(keyStr, childNode);
            }
        }

        return rootNode;
    }

    /**
     * Determina o nome do nó baseado no nome fornecido, namespace e anotações da classe.
     */
    private String determineNodeName(String name, String namespace, Class<?> clazz) {
        String nodeName = name != null ? name : clazz.getSimpleName().toLowerCase();

        if (clazz.isAnnotationPresent(RootElement.class) && namespace == null) {
            RootElement metadata = clazz.getDeclaredAnnotation(RootElement.class);

            if (!metadata.name().isEmpty()) {
                nodeName = metadata.name();
            }

            if (!metadata.namespace().isEmpty()) {
                nodeName = metadata.namespace() + ":" + nodeName;
            }
        } else if (namespace != null && !namespace.isEmpty()) {
            nodeName = namespace + ":" + nodeName;
        }

        return nodeName;
    }

    /**
     * Determina o nome do elemento baseado nas anotações do campo.
     */
    private String determineFieldName(Field field) {
        String fieldName = field.getName();

        if (field.isAnnotationPresent(Element.class)) {
            Element metadata = field.getAnnotation(Element.class);

            if (!metadata.name().isEmpty()) {
                fieldName = metadata.name();
            }

            if (!fieldName.isEmpty() && !metadata.namespace().isEmpty()) {
                fieldName = metadata.namespace() + ":" + fieldName;
            }
        }

        return fieldName;
    }

    /**
     * Extrai o prefixo de namespace de um nome qualificado.
     */
    private String extractNamespacePrefix(String qualifiedName) {
        if (qualifiedName != null && qualifiedName.contains(":")) {
            return qualifiedName.split(":")[0];
        }
        return null;
    }

    /**
     * Extrai o nome local de um nome qualificado.
     */
    private String extractLocalName(String qualifiedName) {
        if (qualifiedName != null && qualifiedName.contains(":")) {
            return qualifiedName.split(":")[1];
        }
        return qualifiedName;
    }

    /**
     * Obtém o namespace declarado em um campo.
     */
    private String getNamespaceFromField(Field field) {
        if (!field.isAnnotationPresent(Element.class)) {
            return null;
        }

        Element annotation = field.getAnnotation(Element.class);
        return annotation.namespace().isEmpty() ? null : annotation.namespace();
    }

    /**
     * Adiciona atributos ao nó com base nos atributos declarados no objeto.
     */
    private void addAttributesToNode(XMLNode node, Class<?> clazz, Object obj)
        throws InvocationTargetException, IllegalAccessException {
        Map<String, String> attributes = Attributes.getAttributes(clazz, obj);

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String[] attributeParts = entry.getValue().split("=");
            if (attributeParts.length == 2) {
                String attrName = attributeParts[0].trim();
                String attrValue = attributeParts[1].trim()
                    .replace("\"", "")
                    .replace("'", "");

                node.addAttribute(attrName, attrValue);
            }
        }
    }

    /**
     * Adiciona declarações de namespace ao nó com base nas anotações da classe.
     */
    private void addNamespacesToNode(XMLNode node, Class<?> clazz) {
        // Adiciona namespaces explícitos
        if (clazz.isAnnotationPresent(Namespaces.class)) {
            Namespaces namespacesAnnotation = clazz.getDeclaredAnnotation(Namespaces.class);

            for (Namespace ns : namespacesAnnotation.namespaces()) {
                if (!ns.name().isEmpty() && !ns.value().isEmpty()) {
                    node.addAttribute("xmlns:" + ns.name(), ns.value());
                }
            }
        }

        // Adiciona namespaces declarados
        if (clazz.isAnnotationPresent(DeclaredNamespaces.class)) {
            DeclaredNamespaces aliases = clazz.getDeclaredAnnotation(DeclaredNamespaces.class);

            for (String alias : aliases.aliases()) {
                if (namespaces.containsKey(alias)) {
                    node.addAttribute("xmlns:" + alias, namespaces.get(alias));
                }
            }
        }

        // Adiciona namespace único
        if (clazz.isAnnotationPresent(Namespace.class)) {
            Namespace ns = clazz.getDeclaredAnnotation(Namespace.class);

            if (!ns.name().isEmpty() && !ns.value().isEmpty()) {
                node.addAttribute("xmlns:" + ns.name(), ns.value());
            }
        }
    }

    /**
     * Registra um namespace para uso futuro.
     */
    public BasicXmlizer registerNamespace(String name, String value) {
        if (name != null && !name.isEmpty() && value != null && !value.isEmpty()) {
            namespaces.put(name, value);
        }
        return this;
    }

    /**
     * Registra múltiplos namespaces para uso futuro.
     */
    public BasicXmlizer registerNamespaces(Map<String, String> namespacesMap) {
        if (namespacesMap != null) {
            namespacesMap.forEach(this::registerNamespace);
        }
        return this;
    }
}