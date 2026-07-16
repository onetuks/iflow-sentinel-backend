package com.onetuks.iflow_sentinel.rule.dto;

import com.onetuks.iflow_sentinel.rule.domain.rule.Rule;
import java.util.Map;
import org.springframework.data.domain.Page;

public record RuleResponse(
    Long id,
    String ruleKey,
    Boolean isGlobal,
    Long customProjectId,
    String type,
    String severity,
    Map<String, Object> target,
    Map<String, Object> params,
    String message,
    boolean enabled
) {

  public static RuleResponse from(Rule entity) {
    return new RuleResponse(
        entity.getId(),
        entity.getRuleKey(),
        entity.getIsGlobal(),
        entity.getCustomProject() != null ? entity.getCustomProject().getId() : null,
        entity.getType() != null ? entity.getType().name() : null,
        entity.getSeverity() != null ? entity.getSeverity().name() : null,
        entity.getTarget(),
        entity.getParams(),
        entity.getMessage(),
        entity.isEnabled()
    );
  }

  public record RuleResponses(
      Page<RuleResponse> responses
  ) {

    public static RuleResponses from(Page<Rule> entities) {
      return new RuleResponses(entities.map(RuleResponse::from));
    }
  }
}
