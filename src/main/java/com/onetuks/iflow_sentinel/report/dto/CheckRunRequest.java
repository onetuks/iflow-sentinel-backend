package com.onetuks.iflow_sentinel.report.dto;

public record CheckRunRequest(Long projectId, String artifactId) {

    public CheckRunRequest(Long projectId, Long artifactId) {
        this(projectId, artifactId != null ? String.valueOf(artifactId) : null);
    }
}
