package com.onetuks.iflow_sentinel.parser.xml;

import com.onetuks.iflow_sentinel.parser.ParserException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SAP iFlow XML(BPMN2/.mmap/.wsdl 등)을 위한 공용 DOM 헬퍼.
 * 네임스페이스 비인식(기본) 모드로 파싱하고, 태그/속성은 로컬네임 접미사 문자열 매칭으로 처리한다.
 * SAP가 내보내는 XML은 bpmn2:/ifl: 접두어를 고정적으로 사용하므로 이 방식으로 충분히 견고하다.
 */
public final class XmlDom {

    private XmlDom() {
    }

    public static Document parse(byte[] xmlBytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));
            doc.getDocumentElement().normalize();
            return doc;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ParserException("XML을 파싱할 수 없습니다.", e);
        }
    }

    /** parent의 직계 자식 요소 중 로컬 태그명이 일치하는 것들을 문서 순서대로 반환한다. */
    public static List<Element> directChildElements(Element parent, String localTagName) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && localNameOf((Element) node).equals(localTagName)) {
                result.add((Element) node);
            }
        }
        return result;
    }

    public static Optional<Element> firstDirectChild(Element parent, String localTagName) {
        List<Element> matches = directChildElements(parent, localTagName);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    /** "bpmn2:participant" -&gt; "participant" 처럼 네임스페이스 접두어를 제거한 로컬 태그명. */
    public static String localNameOf(Element element) {
        String tag = element.getTagName();
        int idx = tag.indexOf(':');
        return idx < 0 ? tag : tag.substring(idx + 1);
    }

    /** 요소의 텍스트 내용을 반환한다. 요소가 없거나(Optional.empty) 자기종료 태그면 빈 문자열. */
    public static String textOf(Optional<Element> element) {
        return element.map(Node::getTextContent).map(String::trim).orElse("");
    }

    public static String attr(Element element, String name) {
        return element.hasAttribute(name) ? element.getAttribute(name) : "";
    }

    /** root 아래 모든 깊이에서 로컬 태그명이 일치하는 요소를 문서 순서대로 재귀 탐색한다. */
    public static List<Element> allByLocalName(Node root, String localName) {
        List<Element> result = new ArrayList<>();
        collectByLocalName(root, localName, result);
        return result;
    }

    private static void collectByLocalName(Node node, String localName, List<Element> out) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) child;
                if (localNameOf(element).equals(localName)) {
                    out.add(element);
                }
                collectByLocalName(element, localName, out);
            }
        }
    }

    /** el의 조상 중 로컬 태그명이 일치하는 첫 요소의 id 속성을 반환한다. 없으면 빈 문자열. */
    public static String ancestorIdByLocalName(Element el, String localName) {
        Node parent = el.getParentNode();
        while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
            Element parentElement = (Element) parent;
            if (localNameOf(parentElement).equals(localName)) {
                return attr(parentElement, "id");
            }
            parent = parent.getParentNode();
        }
        return "";
    }
}
