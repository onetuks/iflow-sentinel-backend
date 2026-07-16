package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import org.springframework.data.domain.Page;

public record ArtifactResponse(
    Long id,
    String sapArtifactId,
    String name,
    String version,
    String type
) {

  public static ArtifactResponse from(Artifact entity) {
    return new ArtifactResponse(
        entity.getId(),
        entity.getSapArtifactId(),
        entity.getName(),
        entity.getVersion(),
        entity.getType().name()
    );
  }

  public record ArtifactResponses(
      Page<ArtifactResponse> responses
  ) {

    public static ArtifactResponses from(Page<Artifact> entities) {
      return new ArtifactResponses(entities.map(ArtifactResponse::from));
    }
  }
}
