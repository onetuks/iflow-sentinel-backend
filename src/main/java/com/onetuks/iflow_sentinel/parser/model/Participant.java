package com.onetuks.iflow_sentinel.parser.model;

/**
 * bpmn2:participant에서 추출한 송·수신 시스템 또는 프로세스 풀.
 * type은 SAP 원문(예: "EndpointRecevier" 오탈자 포함)을 그대로 보존하고,
 * role은 Parser가 정규화한 파생 값("sender"/"receiver"/"process")이다.
 */
public record Participant(
        String id,
        String name,
        String type,
        String role,
        String processRef,
        String enableBasicAuthentication
) {
}
