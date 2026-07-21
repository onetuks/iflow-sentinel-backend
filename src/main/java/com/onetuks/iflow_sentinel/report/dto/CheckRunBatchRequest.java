package com.onetuks.iflow_sentinel.report.dto;

/** CHK-002: 패키지 하위 전체 아티팩트를 일괄 검사한다. */
public record CheckRunBatchRequest(Long projectId, Long integrationPackageId) {
}
