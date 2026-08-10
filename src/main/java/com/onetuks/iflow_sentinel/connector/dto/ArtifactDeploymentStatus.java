package com.onetuks.iflow_sentinel.connector.dto;

/** design-time/runtime 아티팩트 존재 여부를 ID 기준으로 병합해 산출하는 배포 상태. */
public enum ArtifactDeploymentStatus {
    /** design-time과 runtime 모두 존재 */
    DEPLOYED,
    /** design-time만 존재 (아직 배포되지 않음) */
    NOT_DEPLOYED,
    /** runtime만 존재 (design-time 아티팩트가 없는 상태로 배포만 남아있음) */
    INACTIVE
}
