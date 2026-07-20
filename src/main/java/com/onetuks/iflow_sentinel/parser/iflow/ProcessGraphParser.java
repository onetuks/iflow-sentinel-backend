package com.onetuks.iflow_sentinel.parser.iflow;

import com.onetuks.iflow_sentinel.parser.model.EventNode;
import com.onetuks.iflow_sentinel.parser.model.GatewayNode;
import com.onetuks.iflow_sentinel.parser.model.ProcessNode;
import com.onetuks.iflow_sentinel.parser.model.SequenceFlow;
import com.onetuks.iflow_sentinel.parser.xml.IflPropertyExtractor;
import com.onetuks.iflow_sentinel.parser.xml.XmlDom;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * bpmn2:process 하나의 뼈대(프로세스 자체, start/end 이벤트, 게이트웨이, sequenceFlow)를 추출한다.
 * 스텝(callActivity/serviceTask)은 {@link StepParser}가 별도로 처리한다.
 */
public final class ProcessGraphParser {

    private static final Pattern CNAME = Pattern.compile("cname::([^/]+)");

    private ProcessGraphParser() {
    }

    public static ProcessNode parseProcessNode(Element process) {
        String id = XmlDom.attr(process, "id");
        String name = XmlDom.attr(process, "name");
        Map<String, String> props = IflPropertyExtractor.extract(process);
        String type = deriveType(props.get("cmdVariantUri"));
        return new ProcessNode(id, name, type, props.get("processType"), props.get("transactionalHandling"), props.get("transactionTimeout"));
    }

    public static List<EventNode> parseEvents(Element process, String processId) {
        List<EventNode> events = new ArrayList<>();
        for (Element startEvent : XmlDom.directChildElements(process, "startEvent")) {
            events.add(toEventNode(startEvent, "start", processId));
        }
        for (Element endEvent : XmlDom.directChildElements(process, "endEvent")) {
            events.add(toEventNode(endEvent, "end", processId));
        }
        return events;
    }

    private static EventNode toEventNode(Element element, String kind, String processId) {
        String id = XmlDom.attr(element, "id");
        String name = XmlDom.attr(element, "name");
        Map<String, String> props = IflPropertyExtractor.extract(element);
        return new EventNode(id, name, kind, props.get("activityType"), processId);
    }

    public static List<GatewayNode> parseGateways(Element process, String processId) {
        List<GatewayNode> gateways = new ArrayList<>();
        for (Element gateway : XmlDom.directChildElements(process, "exclusiveGateway")) {
            String id = XmlDom.attr(gateway, "id");
            String name = XmlDom.attr(gateway, "name");
            String defaultFlow = XmlDom.attr(gateway, "default");
            Map<String, String> props = IflPropertyExtractor.extract(gateway);
            gateways.add(new GatewayNode(id, name, props.get("activityType"), defaultFlow, props.get("throwException"), processId));
        }
        return gateways;
    }

    /** 게이트웨이 id -&gt; 그 게이트웨이의 기본 sequenceFlow id. sequenceFlow.isDefault 계산에 사용. */
    public static Map<String, String> defaultFlowByGatewayId(List<GatewayNode> gateways) {
        Map<String, String> map = new HashMap<>();
        for (GatewayNode gateway : gateways) {
            if (gateway.defaultFlow() != null && !gateway.defaultFlow().isBlank()) {
                map.put(gateway.id(), gateway.defaultFlow());
            }
        }
        return map;
    }

    public static List<SequenceFlow> parseSequenceFlows(Element process, String processId, Map<String, String> defaultFlowByGatewayId) {
        List<SequenceFlow> flows = new ArrayList<>();
        for (Element flow : XmlDom.directChildElements(process, "sequenceFlow")) {
            String id = XmlDom.attr(flow, "id");
            String name = XmlDom.attr(flow, "name");
            String sourceRef = XmlDom.attr(flow, "sourceRef");
            String targetRef = XmlDom.attr(flow, "targetRef");
            String condition = XmlDom.textOf(XmlDom.firstDirectChild(flow, "conditionExpression"));
            String defaultForSource = defaultFlowByGatewayId.get(sourceRef);
            boolean isDefault = defaultForSource != null && defaultForSource.equals(id);
            flows.add(new SequenceFlow(id, name, sourceRef, targetRef, condition.isBlank() ? null : condition, isDefault, processId));
        }
        return flows;
    }

    private static String deriveType(String cmdVariantUri) {
        if (cmdVariantUri == null) {
            return null;
        }
        Matcher matcher = CNAME.matcher(cmdVariantUri);
        return matcher.find() ? matcher.group(1) : null;
    }
}
