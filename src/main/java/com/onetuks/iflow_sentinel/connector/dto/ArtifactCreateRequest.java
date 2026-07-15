package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactType;

public record ArtifactCreateRequest(
    Long integrationPackageId,
    String sapArtifactId,
    String name,
    String version,
    ArtifactType type
) {}
