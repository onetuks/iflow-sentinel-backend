package com.onetuks.iflow_sentinel.connector.dto;

/**
 * design-time/runtime을 병합한 아티팩트 추적 응답. runtime에만 존재하는(INACTIVE) 항목은 SAP 응답에
 * 패키지 정보가 없으므로 packageId/packageName이 null일 수 있다.
 */
public record TrackerArtifactResponse(
        String packageId,
        String packageName,
        String artifactId,
        String artifactName,
        String version,
        String runtimeStatus,
        ArtifactDeploymentStatus status) {
}
