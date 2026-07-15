package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactType;

public record ArtifactUpdateRequest(
    String name,
    String version,
    ArtifactType type
) {}
