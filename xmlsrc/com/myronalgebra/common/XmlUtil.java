package com.myronalgebra.common;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class XmlUtil {
    public static String error;

    public static Document createDocument() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.newDocument();
        } catch (ParserConfigurationException e) {
        }
        return null;
    }

    /**
     * Reads a File into a Document.
     * @param file
     * @return Document; if null, XmlUtil.error contains reason
     */
    public static Document readDocument(File file) {
        InputStream stream = null;
        try {
            error = null;
            stream = new FileInputStream(file);
            Document doc = readDocument(stream);
            return doc;
        } catch (Exception e) {
            error = e.getMessage();
            return null;
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException e) {
                error = e.getMessage();
                return null;
            }
        }
    }

    /**
     * Reads an XML document from a stream. Unconditionally closes stream.
     * @param stream
     * @return Document; if null, XmlUtil.error contains reason
     */
    public static Document readDocument(InputStream stream) {
        DocumentBuilder dBuilder;
        try {
            error = null;
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(stream);
            return doc;
        } catch (Exception e) {
            error = e.getMessage();
            return null;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Reads a document from a Reader. Unconditionally closes reader.
     * @param reader
     * @return Document; if null, XmlUtil.error contains reason
     */
    public static Document readDocument(Reader reader) {
        DocumentBuilder dBuilder;
        try {
            error = null;
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(reader));
            return doc;
        } catch (Exception e) {
            error = e.getMessage();
            return null;
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                error = e.getMessage();
            }
        }
    }

    /**
     * Writes a document to a file
     * @param doc
     * @param file
     * @param indent true if output should have indentation
     * @throws Exception
     */
    public static void writeDocument(Document doc, File file, boolean indent) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory
                .newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        if (indent)
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }

    public static void writeDocument(Document doc, OutputStream stream, boolean indent) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory
                .newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        if (indent) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        }
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(stream);
        transformer.transform(source, result);
    }

    public static void writeDocument(Document doc, StringWriter stream, int indent) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory
                .newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        if (indent > 0) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", ""+indent);
        }
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(stream);
        transformer.transform(source, result);
    }

    public static void writeDocument(Node node, StringWriter stream, int indent) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory
                .newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        if (indent > 0) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", ""+indent);
        }
        DOMSource source = new DOMSource(node);
        StreamResult result = new StreamResult(stream);
        transformer.transform(source, result);
    }

    public static String toString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            writeDocument(node, sw, -1);
            String result = sw.toString();
            if (result.startsWith("<?xml"))
                result = result.substring(result.indexOf(">")+1);
            return result;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * Return the first Element with the given name;
     * @param parent
     * @param name
     * @return
     */
    public static Element getChildByName(Element parent, String name) {
        Node node = parent.getFirstChild();
        while (node != null) {
            if (node instanceof Element)
                if (((Element)node).getNodeName().equals(name))
                    return (Element)node;
            node = node.getNextSibling();
        }
        return null;
    }

    public static Element getElementByAttr(Document navdoc, String name, String value) {
        return getElementByAttr(navdoc.getDocumentElement(), "id", value);
    }

    public static Element getElementByAttr(Element element, String name, String value) {
        for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element) {
                Element elt = (Element) node;
                String nodeId = elt.getAttribute(name);
                if (nodeId.equals(value))
                    return elt;
                Element result = getElementByAttr(elt, name, value);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    public static class ChildrenByNameIterator implements Iterable<Element>, Iterator<Element> {
        private Element current;
        private String name;

        public ChildrenByNameIterator(Element parent, String name) {
            this.current = getChildByName(parent, name);
            this.name = name;
        }
        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public Element next() {
            Element result = current;
            current = getNextSiblingByName(current, name);
            return result;
        }

        @Override
        public void remove() {
            // TODO Auto-generated method stub

        }
        @Override
        public Iterator<Element> iterator() {
            // TODO Auto-generated method stub
            return this;
        }

    }

    /**
     * Enables iteration over immediate children with given name.
     * Skips text nodes and childer with other names.
     * @param parent
     * @param name
     * @return
     */
    public static Iterable<Element> getChildrenByName(Element parent, String name) {
        return new ChildrenByNameIterator(parent, name);
    }

    public static class NodeListIterator implements Iterable<Element>, Iterator<Element> {

        private NodeList list;
        private int index;

        public NodeListIterator(NodeList list) {
            this.list = list;
            this.index = -1;
        }
        @Override
        public Iterator<Element> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return index < list.getLength() - 1;
        }

        @Override
        public Element next() {
            return (Element)list.item(++index);
        }

        @Override
        public void remove() {
        }

    }

    public static Iterable<Element> getElementsByTagName(Element root, String tagName) {
        return new NodeListIterator(root.getElementsByTagName(tagName));
    }


    /**
     * Returns the first child Node that is an Element.
     * @param parent
     * @return
     */
    public static Element getFirstChildElement(Element parent) {
        Node n = parent.getFirstChild();
        while (n != null && !(n instanceof Element))
            n = n.getNextSibling();
        return (Element)n;
    }
    /**
     * Returns the next sibling element skipping over intervening non-Element nodes (whitespace, but could be text).
     * @param elt
     * @return an Element or null
     */
    public static Element getNextSiblingElement(Element elt) {
        Node n = elt.getNextSibling();
        while (n != null && !(n instanceof Element))
            n = n.getNextSibling();
        return (Element)n;
    }

    /**
     * Returns the previous sibling element skipping over intervening non-Element nodes (whitespace, but could be text).
     * @param elt
     * @return an Element or null
     */
    public static Element getPreviousSiblingElement(Element elt) {
        Node n = elt.getPreviousSibling();
        while (n != null && !(n instanceof Element))
            n = n.getPreviousSibling();
        return (Element)n;
    }

    /**
     * Returns the child of parent with the required name. If the child does not
     * exist or is not the first non-text element, it is created as the first child of the parent.
     *
     * @param parent
     * @param name
     * @return
     */
    public static Element getOrCreateFirst(Element parent, String name) {
        Element element = getFirstChildElement(parent, name);
        if (element != null)
            return element;
        Element newElement = parent.getOwnerDocument().createElement(name);
        insertFirst(parent, newElement);
        return newElement;
    }

    public Element getOrCreateAfter(Element parent, String name, Element refElement) {
        Element element = getFirstChildElement(parent, name);
        if (element != null)
            return element;
        Element newElement = parent.getOwnerDocument().createElement(name);
        insertAfter(parent, newElement, refElement);
        return newElement;
    }

    /**
     * Returns the first non-text element if it matches given name.
     * Return null if no non-text elements exist of if the first Element node does not match given name.
     * @param parent
     * @param name
     * @return
     */
    public static Element getFirstChildElement(Element parent, String name) {
        Node node = parent.getFirstChild();
        while (node != null) {
            if (node instanceof Element)
                break;
            node = node.getNextSibling();
        }
        if (node == null)
            return null;
        if (node.getNodeName().equals(name))
            return (Element)node;
        return null;
    }

    /**
     * Inserts newElement as the first child of parent.
     *
     * @param parent
     * @param newElement
     * @return inserted element
     */
    public static Element insertFirst(Element parent, Element newElement) {
        Node first = parent.getFirstChild();
        if (first != null)
            parent.insertBefore(newElement, first);
        else
            parent.appendChild(newElement);
        return newElement;
    }

    /**
     * Inserts newElement after refElement.
     *
     * @param parent the element into whose child list the new element will be inserted
     * @param newElement the element to be inserted
     * @param refElement the element after which newElement will be inserted
     * @return newElement
     */
    public static Element insertAfter(Element parent, Element newElement,
                                      Element refElement) {
        Node sibling = refElement.getNextSibling();
        if (sibling == null)
            parent.appendChild(newElement);
        else
            parent.insertBefore(newElement, sibling);
        return newElement;
    }

    public static Element append(Element parent, String nodename, String ... attributes) {
        if (attributes.length %2 != 0)
            throw new IllegalArgumentException("XmlUtil.append: expecting name-value pairs");
        Element elt = parent.getOwnerDocument().createElement(nodename);
        for (int i = 0; i < attributes.length; i += 2) {
            elt.setAttribute(attributes[i], attributes[i+1]);
        }
        parent.appendChild(elt);
        return elt;
    }

    /**
     * gets next sibling element with the required name skipping over intervening siblings
     * @param elt
     * @param name
     * @return
     */
    public static Element getNextSiblingByName(Element elt, String name) {
        elt = getNextSiblingElement(elt);
        while (elt != null && !elt.getNodeName().equals(name))
            elt = getNextSiblingElement(elt);
        return elt;
    }

    /**
     * gets the previous sibling element with the required name skipping over interening siblings
     * @param elt
     * @param name
     * @return
     */
    public static Element getPreviousSiblingByName(Element elt, String name) {
        elt = getPreviousSiblingElement(elt);
        while (elt != null && !elt.getNodeName().equals(name))
            elt = getPreviousSiblingElement(elt);
        return elt;
    }

    public static String toString(Element item, boolean removeWhiteSpace) {
        StringBuffer buffer = new StringBuffer();
        toString(item, buffer, removeWhiteSpace);
        String result = buffer.toString();
//		result = result.replaceAll("\"", "&quot;");
//		result = result.replaceAll("\n", " ");
        return result;
    }

    private static void toString(Element item, StringBuffer buffer, boolean removeWhiteSpace) {
        buffer.append("<");
        buffer.append(item.getNodeName());
        NamedNodeMap attrs = item.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr)attrs.item(i);
            buffer.append(" ");
            buffer.append(attr.getName());
            buffer.append("=\"");
            buffer.append(attr.getValue());
            buffer.append("\"");
        }
        if (item.hasChildNodes()) {
            buffer.append(">");
            for (Node n = item.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element)
                    toString((Element)n, buffer, removeWhiteSpace);
                else if (n.getNodeType() == Node.TEXT_NODE && removeWhiteSpace && n.getTextContent().matches("[ \t\r\n]*"))
                    continue;
                else
                    buffer.append(n.getTextContent().trim());
            }
            buffer.append("</");
            buffer.append(item.getNodeName());
            buffer.append(">");
        } else {
            buffer.append("/>");
        }
    }

    /**
     * Creates a copy of a Node from one document in another.
     * The copy is recursive, with names and attributes of subnodes copied as well.
     * @param doc target document
     * @param name new name for node
     * @param source to be copied
     * @param copyAttributes true if attributes are copied.
     * @return
     */
    public static Element cloneAs(Document doc, String name, Element source, boolean copyAttributes) {
        Element e = doc.createElement(name);
        if (copyAttributes) {
            copyAttributes(e, source);
        }
        for (Node n = source.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.TEXT_NODE)
                e.appendChild(doc.createTextNode(n.getTextContent()));
            else if (n.getNodeType() == Node.ELEMENT_NODE)
                e.appendChild(cloneAs(doc, n.getNodeName(), (Element)n, true));
        }
        return e;
    }

    /**
     * Creates a new Element and moves all children from the source Element
     * to the new Element.
     * @param doc
     * @param name
     * @param source
     * @return
     */
    public static Element moveTo(Document doc, String name, Element source) {
        Element e = doc.createElement(name);
        copyAttributes(e, source);
        while (source.hasChildNodes()) {
            Node n = source.removeChild(source.getFirstChild());
            e.appendChild(n);
        }
        return e;
    }

    /**
     * Copies element and text nodes contained within source and appends them to target.
     * @param doc
     * @param target
     * @param source
     */
    public static void copyTo(Element target, Element source) {
        for (Node n = source.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                Element copy = target.getOwnerDocument().createElement(((Element) n).getTagName());
                target.appendChild(copy);
                copyAttributes(copy, n);
                copyTo(copy, (Element) n);
            } else if (n.getNodeType() == Node.TEXT_NODE) {
                target.appendChild(target.getOwnerDocument().createTextNode(n.getTextContent()));
            }
        }
    }

    /**
     * Creates a new Node and moves all children starting with firstOfMany to the new Node.
     *
     * @param doc
     * @param name
     * @param firstOfMany
     * @return
     */
    public static Element moveWithSiblingsTo(Document doc, String name, Node firstOfMany) {
        Element e = doc.createElement(name);
        while(firstOfMany != null) {
            Node next = firstOfMany.getNextSibling();
            Node n = firstOfMany.getParentNode().removeChild(firstOfMany);
            e.appendChild(n);
            firstOfMany = next;
        }
        return e;
    }

    /**
     * Wraps the children of an element in a new element. &lt;parent>&lt;a/>&lt;b/>&lt;/parent>
     * becomes &lt;parent>&lt;wrapper>&lt;a/>&lt;b/>&lt;/wrapper>&lt;/parent>
     * @param elementName
     * @param parent
     * @return newly created wrapper; new child of parent
     */
    public static Element insertWrapper(String elementName, Element parent) {
        Element wrapper = parent.getOwnerDocument().createElement(elementName);
        while (parent.hasChildNodes()) {
            Node child = parent.getFirstChild();
            parent.removeChild(child);
            wrapper.appendChild(child);
        }
        parent.appendChild(wrapper);
        return wrapper;
    }

    /**
     * Removes an element by moving all its children to its place.
     * &lt;parent>...&lt;wrapper>&lt;child1/>&lt;child2/>&lt;/wrapper>...&lt;/parent>
     * becomes &lt;parent>...&lt;child1/>&lt;child2/>...&lt;/parent>
     * @param element
     */
    public static void removeWrapper(Element element) {
        Element parent = (Element) element.getParentNode();
        while (element.getFirstChild() != null) {
            Node n = element.getFirstChild();
            element.removeChild(n);
            parent.insertBefore(n, element);
        }
//		Node prev = null;
//		for (Node n = element.getLastChild(); n != null; n = prev) {
//			prev = n.getPreviousSibling();
//			element.removeChild(n);
//			parent.insertBefore(n, element);
//		}
        parent.removeChild(element);
    }

    /**
     * Replaces an element with a new element.
     * <br/>&lt;parent>...&lt;node>&lt;next>...&lt;/parent> becomes &lt;parent>...&lt;wrapper>&lt;node>&lt;/wrapper>&lt;next>...&lt;/parent>
     * @param elementName name of new wrapper element
     * @param node to be wrapped
     * @return new node
     */
    public static Element wrap(String elementName, Element node) {
        Node parent = node.getParentNode();
        Node next = node.getNextSibling();
        node.getParentNode().removeChild(node);
        Element wrapper = node.getOwnerDocument().createElement(elementName);
        wrapper.appendChild(node);
        if (next == null)
            parent.appendChild(wrapper);
        else
            parent.insertBefore(wrapper, next);
        return wrapper;
    }

    /**
     * Finds all element using getElementsByTagName and
     * returns a copy of the NodeList.
     * Use this method when altering elements the NodeList causes nodes
     * to be skipped.
     * @param body
     * @param elementName
     * @return
     */
    public static Element[] copyElements(Element body, String elementName) {
        NodeList refs = body.getElementsByTagName(elementName);
        Element[] refList = new Element[refs.getLength()];
        for (int i = 0; i < refs.getLength(); i++)
            refList[i] = (Element) refs.item(i);
        return refList;
    }

    public static Element [] getElementList(Element body, String elementName) {
        ArrayList<Element> list = new ArrayList<>();
        getElementsByTagName(list, body, elementName);
        return list.toArray(new Element[list.size()]);
    }

    private static void getElementsByTagName(ArrayList<Element> list, Element body, String elementName) {
        for (Node n = body.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (n.getNodeName().equals(elementName)) {
                    list.add((Element) n);
                }
                getElementsByTagName(list, (Element) n, elementName);
            }
        }
    }

    /**
     * Returns the first child that matches
     * element, attribute and value.
     * @param root
     * @param tag
     * @param attribute
     * @param value
     * @return
     */
    public static Element GetChildWithAttribute(Element root, String tag,
                                                String attribute, String value) {
        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                if (((Element)n).getTagName().equals(tag) && ((Element)n).getAttribute(attribute).equals(value))
                    return (Element)n;
            }
        }
        return null;
    }

    /**
     * Copies all the attributes to a (new/'nother) node
     * @param target
     * @param source
     */
    public static void copyAttributes(Element target, Node source) {
        NamedNodeMap attrs = source.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr)attrs.item(i);
            target.setAttribute(attr.getName(), attr.getValue());
        }
    }

    public static Element readNode(Document doc, String name, Reader reader) {
        Document nodeDoc = readDocument(reader);
        return cloneAs(doc, nodeDoc.getDocumentElement().getNodeName(), nodeDoc.getDocumentElement(), true);
    }


}