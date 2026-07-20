package com.onetuks.iflow_sentinel.parser.model;

import java.util.Map;

/**
 * bpmn2:callActivity / bpmn2:serviceTask에서 추출한 흐름 스텝.
 * type(activityType)에 대응하는 정규화 필드 하나만 채워지고 나머지 네 개는 null이다:
 * Mapping→mapping, Script→script, Enricher→enricher, DBstorage→store, ProcessCallElement→call.
 * ExternalCall(serviceTask)은 다섯 필드 모두 null이며, 연결된 채널(sourceRef==이 스텝의 id)로 식별한다.
 */
public record StepNode(
        String id,
        String name,
        String type,
        String processId,
        String componentVersion,
        Map<String, String> properties,
        MappingRef mapping,
        ScriptRef script,
        EnricherRef enricher,
        StoreRef store,
        CallRef call
) {
    public StepNode {
        properties = Map.copyOf(properties);
    }
}
