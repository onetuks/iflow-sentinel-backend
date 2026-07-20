package com.onetuks.iflow_sentinel.parser.model;

/** bpmn2:sequenceFlow. isDefault는 소속 프로세스 내 게이트웨이의 default 속성과 대조해 Parser가 계산한 파생 값. */
public record SequenceFlow(
        String id,
        String name,
        String sourceRef,
        String targetRef,
        String condition,
        boolean isDefault,
        String processId
) {
}
