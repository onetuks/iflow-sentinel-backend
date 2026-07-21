package com.onetuks.iflow_sentinel.rule.dto;

import java.util.Map;

import com.onetuks.iflow_sentinel.rule.domain.Rule;

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
        boolean enabled) {

    public static RuleResponse from(Rule rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getRuleKey(),
                rule.getIsGlobal(),
                rule.getCustomProject() != null ? rule.getCustomProject().getId() : null,
                rule.getType() != null ? rule.getType().name() : null,
                rule.getSeverity() != null ? rule.getSeverity().name() : null,
                rule.getTarget(),
                rule.getParams(),
                rule.getMessage(),
                rule.isEnabled());
    }
}
