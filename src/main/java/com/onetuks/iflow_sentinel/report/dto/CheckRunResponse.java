package com.onetuks.iflow_sentinel.report.dto;

import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRun;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.data.domain.Page;

public record CheckRunResponse(
    Long id,
    Long projectId,
    LocalDateTime startedAt,
    String status,
    Map<String, Object> summary
) {

  public static CheckRunResponse from(CheckRun entity) {
    return new CheckRunResponse(
        entity.getId(),
        entity.getProject() != null ? entity.getProject().getId() : null,
        entity.getStartedAt(),
        entity.getStatus() != null ? entity.getStatus().name() : null,
        entity.getSummary()
    );
  }

  public record CheckRunResponses(
      Page<CheckRunResponse> responses
  ) {

    public static CheckRunResponses from(Page<CheckRun> entities) {
      return new CheckRunResponses(entities.map(CheckRunResponse::from));
    }
  }
}
