package com.onetuks.iflow_sentinel.parser.xml;

import org.w3c.dom.Element;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * bpmn2:extensionElements &gt; ifl:property(key,value) 맵을 뽑아내는 공용 헬퍼.
 * collaboration/participant/messageFlow/process/callActivity/serviceTask/gateway/sequenceFlow
 * 전부에서 반복되는 패턴이므로 모든 요소 타입 파서가 이 헬퍼 하나를 공유한다.
 */
public final class IflPropertyExtractor {

    private IflPropertyExtractor() {
    }

    public static Map<String, String> extract(Element ownerElement) {
        Optional<Element> ext = XmlDom.firstDirectChild(ownerElement, "extensionElements");
        if (ext.isEmpty()) {
            return Map.of();
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (Element prop : XmlDom.directChildElements(ext.get(), "property")) {
            String key = XmlDom.textOf(XmlDom.firstDirectChild(prop, "key"));
            String value = XmlDom.textOf(XmlDom.firstDirectChild(prop, "value"));
            map.put(key, value);
        }
        return Map.copyOf(map);
    }
}
