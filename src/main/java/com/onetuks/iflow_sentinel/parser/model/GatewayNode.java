package com.onetuks.iflow_sentinel.parser.model;

/** bpmn2:exclusiveGateway 등 라우팅 분기. defaultFlow는 XML의 default 속성값(기본 sequenceFlow의 id). */
public record GatewayNode(
        String id,
        String name,
        String type,
        String defaultFlow,
        String throwException,
        String processId
) {
}
