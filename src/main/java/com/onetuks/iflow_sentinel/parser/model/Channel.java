package com.onetuks.iflow_sentinel.parser.model;

import java.util.List;
import java.util.Map;

/**
 * bpmn2:messageFlow에서 추출한 어댑터 채널.
 * address/auth/externalizedRefs는 Parser가 계산한 파생 필드이며,
 * properties에는 ifl:property 원본 맵 전체를 보존한다(설계서 5.6 — 정규화 필드 + 원본 맵 병행 제공 원칙).
 */
public record Channel(
        String id,
        String name,
        String sourceRef,
        String targetRef,
        String direction,
        String adapterType,
        String transportProtocol,
        String messageProtocol,
        String componentVersion,
        String system,
        String address,
        ChannelAuth auth,
        List<String> externalizedRefs,
        Map<String, String> properties
) {
    public Channel {
        externalizedRefs = List.copyOf(externalizedRefs);
        properties = Map.copyOf(properties);
    }
}
