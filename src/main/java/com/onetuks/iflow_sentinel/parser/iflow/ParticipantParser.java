package com.onetuks.iflow_sentinel.parser.iflow;

import com.onetuks.iflow_sentinel.parser.model.Participant;
import com.onetuks.iflow_sentinel.parser.xml.IflPropertyExtractor;
import com.onetuks.iflow_sentinel.parser.xml.XmlDom;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * bpmn2:participant에서 참여자를 추출한다. role은 ifl:type을 정규화한 파생 값이며,
 * SAP 원본의 "EndpointRecevier" 오탈자는 type에 그대로 보존하되 role에서는 "receiver"로 정규화한다.
 */
public final class ParticipantParser {

    private ParticipantParser() {
    }

    public static List<Participant> parse(Element collaboration) {
        List<Participant> participants = new ArrayList<>();
        for (Element participant : XmlDom.directChildElements(collaboration, "participant")) {
            String id = XmlDom.attr(participant, "id");
            String name = XmlDom.attr(participant, "name");
            String type = XmlDom.attr(participant, "ifl:type");
            String processRef = XmlDom.attr(participant, "processRef");
            Map<String, String> props = IflPropertyExtractor.extract(participant);
            String enableBasicAuthentication = props.get("enableBasicAuthentication");

            participants.add(new Participant(id, name, type, deriveRole(type), processRef, enableBasicAuthentication));
        }
        return participants;
    }

    private static String deriveRole(String type) {
        if (type == null || type.isBlank()) {
            return "";
        }
        if ("IntegrationProcess".equals(type)) {
            return "process";
        }
        String lower = type.toLowerCase(Locale.ROOT);
        if (lower.contains("sender")) {
            return "sender";
        }
        if (lower.contains("receiver") || lower.contains("recevier")) {
            return "receiver";
        }
        return lower;
    }
}
