package com.onetuks.iflow_sentinel.parser.iflow;

import com.onetuks.iflow_sentinel.parser.model.Channel;
import com.onetuks.iflow_sentinel.parser.model.ChannelAuth;
import com.onetuks.iflow_sentinel.parser.util.TokenScanner;
import com.onetuks.iflow_sentinel.parser.xml.IflPropertyExtractor;
import com.onetuks.iflow_sentinel.parser.xml.XmlDom;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * bpmn2:messageFlow에서 어댑터 채널을 추출한다. address/auth/externalizedRefs는 Parser가 계산한 파생 필드다.
 * 처리 중 발견한 {{name}} 참조는 referencedByAccumulator에 (token -&gt; channelId) 형태로 함께 쌓아
 * parameters[].referencedBy 계산에 재사용한다.
 */
public final class ChannelParser {

    private ChannelParser() {
    }

    public static List<Channel> parse(Element collaboration, Map<String, List<String>> referencedByAccumulator) {
        List<Channel> channels = new ArrayList<>();
        for (Element messageFlow : XmlDom.directChildElements(collaboration, "messageFlow")) {
            String id = XmlDom.attr(messageFlow, "id");
            String name = XmlDom.attr(messageFlow, "name");
            String sourceRef = XmlDom.attr(messageFlow, "sourceRef");
            String targetRef = XmlDom.attr(messageFlow, "targetRef");
            Map<String, String> props = IflPropertyExtractor.extract(messageFlow);

            String address = firstNonBlank(props.get("urlPath"), props.get("httpAddressWithoutQuery"));
            ChannelAuth auth = new ChannelAuth(
                    props.get("senderAuthType"),
                    props.get("userRole"),
                    props.get("authenticationMethod"),
                    props.get("credentialName")
            );
            String allValues = String.join(" ", props.values());
            List<String> externalizedRefs = TokenScanner.findExternalizedRefs(allValues);
            TokenScanner.collectInto(referencedByAccumulator, id, allValues);

            channels.add(new Channel(
                    id, name, sourceRef, targetRef,
                    props.get("direction"),
                    props.get("ComponentType"),
                    props.get("TransportProtocol"),
                    props.get("MessageProtocol"),
                    props.get("componentVersion"),
                    props.get("system"),
                    address,
                    auth,
                    externalizedRefs,
                    props
            ));
        }
        return channels;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }
}
