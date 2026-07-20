package com.onetuks.iflow_sentinel.parser.model;

/** bpmn2:startEvent/endEvent에서 추출. kind는 태그로부터 파생한 "start"/"end". */
public record EventNode(
        String id,
        String name,
        String kind,
        String type,
        String processId
) {
}
