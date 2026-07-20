package com.onetuks.iflow_sentinel.parser.parameters;

import com.onetuks.iflow_sentinel.parser.xml.XmlDom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * parameters.propdef(선언: 타입·필수여부·설명)를 파싱한다.
 * &lt;param_references&gt; 블록은 UI 메타데이터이며 스키마에 없으므로 무시한다.
 */
public final class ParametersPropDefParser {

    private ParametersPropDefParser() {
    }

    public static Map<String, ParamDef> parse(byte[] propdefBytes) {
        Document doc = XmlDom.parse(propdefBytes);
        Element root = doc.getDocumentElement();
        Map<String, ParamDef> defs = new LinkedHashMap<>();
        for (Element parameter : XmlDom.directChildElements(root, "parameter")) {
            String name = XmlDom.textOf(XmlDom.firstDirectChild(parameter, "name"));
            String type = XmlDom.textOf(XmlDom.firstDirectChild(parameter, "type"));
            boolean isRequired = "true".equalsIgnoreCase(XmlDom.textOf(XmlDom.firstDirectChild(parameter, "isRequired")));
            String description = XmlDom.textOf(XmlDom.firstDirectChild(parameter, "description"));
            defs.put(name, new ParamDef(type, isRequired, description));
        }
        return defs;
    }

    public record ParamDef(String type, boolean isRequired, String description) {
    }
}
