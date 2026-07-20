package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.domain.artifact.ArtifactType;

public record ArtifactResponse(Long id, Long integrationPackageId, String sapArtifactId, String name, String version, ArtifactType type) {

    public static ArtifactResponse from(Artifact artifact) {
        return new ArtifactResponse(
                artifact.getId(),
                artifact.getIntegrationPackage().getId(),
                artifact.getSapArtifactId(),
                artifact.getName(),
                artifact.getVersion(),
                artifact.getType()
        );
    }
}
