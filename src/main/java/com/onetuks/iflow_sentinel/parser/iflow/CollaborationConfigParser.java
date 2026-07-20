package com.onetuks.iflow_sentinel.parser.iflow;

import com.onetuks.iflow_sentinel.parser.model.IflowConfig;
import com.onetuks.iflow_sentinel.parser.xml.IflPropertyExtractor;
import org.w3c.dom.Element;

import java.util.Map;

/** bpmn2:collaboration의 ifl:property 맵에서 iFlow 전역 설정(IflowConfig)을 추출한다. */
public final class CollaborationConfigParser {

    private CollaborationConfigParser() {
    }

    public static IflowConfig parse(Element collaboration) {
        Map<String, String> props = IflPropertyExtractor.extract(collaboration);
        return new IflowConfig(
                props.get("log"),
                props.get("returnExceptionToSender"),
                props.get("ServerTrace"),
                props.get("httpSessionHandling"),
                props.get("corsEnabled"),
                props.get("allowedOrigins"),
                props.get("allowedHeaderList"),
                props.get("componentVersion"),
                props
        );
    }
}
