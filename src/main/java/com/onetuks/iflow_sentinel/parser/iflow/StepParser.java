package com.onetuks.iflow_sentinel.parser.iflow;

import com.onetuks.iflow_sentinel.parser.model.CallRef;
import com.onetuks.iflow_sentinel.parser.model.EnricherRef;
import com.onetuks.iflow_sentinel.parser.model.EnricherRow;
import com.onetuks.iflow_sentinel.parser.model.MappingRef;
import com.onetuks.iflow_sentinel.parser.model.ScriptRef;
import com.onetuks.iflow_sentinel.parser.model.StepNode;
import com.onetuks.iflow_sentinel.parser.model.StoreRef;
import com.onetuks.iflow_sentinel.parser.util.ScriptLanguage;
import com.onetuks.iflow_sentinel.parser.util.TokenScanner;
import com.onetuks.iflow_sentinel.parser.xml.IflPropertyExtractor;
import com.onetuks.iflow_sentinel.parser.xml.XmlDom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * bpmn2:callActivity / bpmn2:serviceTask에서 흐름 스텝을 추출하고, activityType에 따라
 * 해당 타입의 정규화 필드 하나만 채운다. activityType이 없는 serviceTask는 ExternalCall로 간주한다
 * (실측 샘플에서 serviceTask는 activityType 속성 자체를 갖지 않음을 확인).
 */
public final class StepParser {

    private StepParser() {
    }

    public static List<StepNode> parseSteps(Element process, String processId, Map<String, List<String>> referencedByAccumulator) {
        List<StepNode> steps = new ArrayList<>();
        for (Element callActivity : XmlDom.directChildElements(process, "callActivity")) {
            steps.add(toStepNode(callActivity, processId, referencedByAccumulator));
        }
        for (Element serviceTask : XmlDom.directChildElements(process, "serviceTask")) {
            steps.add(toStepNode(serviceTask, processId, referencedByAccumulator));
        }
        return steps;
    }

    private static StepNode toStepNode(Element element, String processId, Map<String, List<String>> referencedByAccumulator) {
        String id = XmlDom.attr(element, "id");
        String name = XmlDom.attr(element, "name");
        Map<String, String> props = IflPropertyExtractor.extract(element);
        String type = props.getOrDefault("activityType", "serviceTask".equals(XmlDom.localNameOf(element)) ? "ExternalCall" : "");

        TokenScanner.collectInto(referencedByAccumulator, id, String.join(" ", props.values()));

        MappingRef mapping = null;
        ScriptRef script = null;
        EnricherRef enricher = null;
        StoreRef store = null;
        CallRef call = null;

        switch (type) {
            case "Mapping" -> mapping = new MappingRef(
                    props.get("mappingType"), props.get("mappingname"), props.get("mappinguri"), props.get("mappingReference"));
            case "Script" -> {
                String file = props.get("script");
                script = new ScriptRef(file, ScriptLanguage.fromFileName(file), props.get("scriptBundleId"), props.get("scriptFunction"));
            }
            case "Enricher" -> enricher = new EnricherRef(
                    props.get("bodyType"), parseEnricherRows(props.get("headerTable")), parseEnricherRows(props.get("propertyTable")));
            case "DBstorage" -> store = new StoreRef(
                    props.get("operation"), props.get("storageName"), props.get("visibility"),
                    props.get("encrypt"), props.get("expire"), props.get("messageId"));
            case "ProcessCallElement" -> call = new CallRef(props.get("processId"), props.get("subActivityType"));
            default -> {
                // ExternalCall 등 그 외 타입은 정규화 필드가 없다.
            }
        }

        return new StepNode(id, name, type, processId, props.get("componentVersion"), props, mapping, script, enricher, store, call);
    }

    /**
     * Enricher의 headerTable/propertyTable property 값은 &lt;row&gt;&lt;cell id='X'&gt;..&lt;/cell&gt;...&lt;/row&gt;
     * 형태의 XML 문자열을 담고 있다(DOM이 이미 1차 언이스케이프한 텍스트). 여러 row가 형제로 나열되어 그 자체로는
     * well-formed XML이 아니므로 임시 루트로 감싸 2차 DOM 파싱한다.
     */
    static List<EnricherRow> parseEnricherRows(String rawXml) {
        if (rawXml == null || rawXml.isBlank()) {
            return List.of();
        }
        String wrapped = "<rows>" + rawXml + "</rows>";
        Document doc = XmlDom.parse(wrapped.getBytes(StandardCharsets.UTF_8));
        List<EnricherRow> rows = new ArrayList<>();
        for (Element row : XmlDom.directChildElements(doc.getDocumentElement(), "row")) {
            Map<String, String> cells = new HashMap<>();
            for (Element cell : XmlDom.directChildElements(row, "cell")) {
                cells.put(XmlDom.attr(cell, "id"), cell.getTextContent());
            }
            rows.add(new EnricherRow(
                    cells.getOrDefault("Action", ""),
                    cells.getOrDefault("Type", ""),
                    cells.getOrDefault("Value", ""),
                    cells.getOrDefault("Default", ""),
                    cells.getOrDefault("Name", ""),
                    cells.getOrDefault("Datatype", "")
            ));
        }
        return rows;
    }
}
