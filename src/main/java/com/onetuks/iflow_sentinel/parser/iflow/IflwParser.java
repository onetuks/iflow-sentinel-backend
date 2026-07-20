package com.onetuks.iflow_sentinel.parser.iflow;

import com.onetuks.iflow_sentinel.parser.ParserException;
import com.onetuks.iflow_sentinel.parser.model.Channel;
import com.onetuks.iflow_sentinel.parser.model.EventNode;
import com.onetuks.iflow_sentinel.parser.model.ExceptionSubprocess;
import com.onetuks.iflow_sentinel.parser.model.GatewayNode;
import com.onetuks.iflow_sentinel.parser.model.IflowConfig;
import com.onetuks.iflow_sentinel.parser.model.IflowModel;
import com.onetuks.iflow_sentinel.parser.model.Participant;
import com.onetuks.iflow_sentinel.parser.model.ProcessNode;
import com.onetuks.iflow_sentinel.parser.model.SequenceFlow;
import com.onetuks.iflow_sentinel.parser.model.StepNode;
import com.onetuks.iflow_sentinel.parser.xml.XmlDom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * .iflw(BPMN2 XML) 전체를 오케스트레이션하는 얇은 파서. Document를 한 번 열어
 * 하위 파서들(Collaboration/Participant/Channel/ProcessGraph/Step/ExceptionSubprocess)을 호출하고 조립만 한다.
 */
public final class IflwParser {

    private IflwParser() {
    }

    public static IflwParseResult parse(byte[] iflwBytes) {
        Document document = XmlDom.parse(iflwBytes);
        Element definitions = document.getDocumentElement();
        Element collaboration = XmlDom.firstDirectChild(definitions, "collaboration")
                .orElseThrow(() -> new ParserException("bpmn2:collaboration을 찾을 수 없습니다."));

        IflowConfig config = CollaborationConfigParser.parse(collaboration);
        List<Participant> participants = ParticipantParser.parse(collaboration);

        Map<String, List<String>> parameterReferences = new LinkedHashMap<>();
        List<Channel> channels = ChannelParser.parse(collaboration, parameterReferences);

        List<ProcessNode> processes = new ArrayList<>();
        List<EventNode> events = new ArrayList<>();
        List<GatewayNode> gateways = new ArrayList<>();
        List<SequenceFlow> sequenceFlows = new ArrayList<>();
        List<StepNode> steps = new ArrayList<>();

        for (Element process : XmlDom.directChildElements(definitions, "process")) {
            String processId = XmlDom.attr(process, "id");
            processes.add(ProcessGraphParser.parseProcessNode(process));

            List<GatewayNode> processGateways = ProcessGraphParser.parseGateways(process, processId);
            gateways.addAll(processGateways);
            events.addAll(ProcessGraphParser.parseEvents(process, processId));

            Map<String, String> defaultFlowByGatewayId = ProcessGraphParser.defaultFlowByGatewayId(processGateways);
            sequenceFlows.addAll(ProcessGraphParser.parseSequenceFlows(process, processId, defaultFlowByGatewayId));

            steps.addAll(StepParser.parseSteps(process, processId, parameterReferences));
        }

        List<ExceptionSubprocess> exceptionSubprocesses = ExceptionSubprocessParser.parse(document);

        IflowModel model = new IflowModel(config, participants, channels, processes, events, gateways, sequenceFlows, steps, exceptionSubprocesses);
        return new IflwParseResult(model, parameterReferences);
    }

    /** IflowModel 본체와, parameters[].referencedBy 계산에 쓰이는 (파라미터명 -&gt; 참조 요소 id 목록) 부가 산출물. */
    public record IflwParseResult(IflowModel model, Map<String, List<String>> parameterReferences) {
    }
}
