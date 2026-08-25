package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactType;

public record ArtifactResponse(
        String id,
        Long integrationPackageId,
        String sapArtifactId,
        String name,
        String version,
        ArtifactType type) {

    public static ArtifactResponse from(Artifact artifact) {
        return new ArtifactResponse(
                artifact.getSapArtifactId(),
                artifact.getIntegrationPackage() != null ? artifact.getIntegrationPackage().getId() : null,
                artifact.getSapArtifactId(),
                artifact.getName(),
                artifact.getVersion(),
                artifact.getType());
    }
}
