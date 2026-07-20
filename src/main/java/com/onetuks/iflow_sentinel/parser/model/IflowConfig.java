package com.onetuks.iflow_sentinel.parser.model;

import java.util.Map;

/**
 * bpmn2:collaboration의 ifl:property 맵에서 추출한 iFlow 전역 설정.
 * 자주 쓰는 값은 정규화 필드로 노출하되, 원본 맵 전체를 raw로 함께 제공한다(설계서 5.6).
 */
public record IflowConfig(
        String log,
        String returnExceptionToSender,
        String serverTrace,
        String httpSessionHandling,
        String corsEnabled,
        String allowedOrigins,
        String allowedHeaderList,
        String componentVersion,
        Map<String, String> raw
) {
    public IflowConfig {
        raw = Map.copyOf(raw);
    }
}
