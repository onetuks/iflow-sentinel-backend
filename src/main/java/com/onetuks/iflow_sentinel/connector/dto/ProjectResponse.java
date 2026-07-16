package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import org.springframework.data.domain.Page;

public record ProjectResponse(
    Long id,
    String name
) {

  public static ProjectResponse from(Project entity) {
    return new ProjectResponse(
        entity.getId(),
        entity.getName()
    );
  }

  public record ProjectResponses(
      Page<ProjectResponse> responses
  ) {

    public static ProjectResponses from(Page<Project> entities) {
      return new ProjectResponses(entities.map(ProjectResponse::from));
    }
  }
}
