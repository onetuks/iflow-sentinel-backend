package com.onetuks.iflow_sentinel.parser.model;

/**
 * errorEventDefinition을 가진 bpmn2:subProcess. 배열이 비어 있으면 예외 처리가 없다는 뜻이며,
 * required-error-handler 규칙의 직접적 근거가 된다(설계서 5.3.7).
 */
public record ExceptionSubprocess(String id, String name, String processId) {
}
