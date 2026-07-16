package com.onetuks.iflow_sentinel.report.dto;

import com.onetuks.iflow_sentinel.report.domain.finding.Finding;
import org.springframework.data.domain.Page;

public record FindingResponse(
    Long id,
    Long checkRunId,
    Long artifactId,
    Long ruleId,
    String severity,
    String location,
    String message
) {

  public static FindingResponse from(Finding entity) {
    return new FindingResponse(
        entity.getId(),
        entity.getCheckRun() != null ? entity.getCheckRun().getId() : null,
        entity.getArtifact() != null ? entity.getArtifact().getId() : null,
        entity.getRule() != null ? entity.getRule().getId() : null,
        entity.getSeverity() != null ? entity.getSeverity().name() : null,
        entity.getLocation(),
        entity.getMessage()
    );
  }

  public record FindingResponses(
      Page<FindingResponse> responses
  ) {

    public static FindingResponses from(Page<Finding> entities) {
      return new FindingResponses(entities.map(FindingResponse::from));
    }
  }
}
