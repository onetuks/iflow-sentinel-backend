package com.onetuks.iflow_sentinel.rulemgmt.dto;

import com.onetuks.iflow_sentinel.domain.rule.Rule;
import com.onetuks.iflow_sentinel.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.domain.rule.Severity;

import java.util.Map;

public record RuleResponse(
        Long id,
        String ruleKey,
        Long rulesetId,
        RuleType type,
        Severity severity,
        Map<String, Object> target,
        Map<String, Object> params,
        String message,
        boolean enabled
) {
    public static RuleResponse from(Rule rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getRuleKey(),
                rule.getRuleset() == null ? null : rule.getRuleset().getId(),
                rule.getType(),
                rule.getSeverity(),
                rule.getTarget(),
                rule.getParams(),
                rule.getMessage(),
                rule.isEnabled()
        );
    }
}
