package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.rule.domain.rule.Rule;

/** PJR-005: 프로젝트에 적용 가능한 규칙(전역+프로젝트 전용)과 현재 적용 여부 현황. */
public record ProjectRuleResponse(
        Long ruleId,
        String ruleKey,
        boolean isGlobal,
        String type,
        String severity,
        String message,
        boolean isEnabled
) {
    public static ProjectRuleResponse of(Rule rule, boolean isEnabled) {
        return new ProjectRuleResponse(
                rule.getId(),
                rule.getRuleKey(),
                Boolean.TRUE.equals(rule.getIsGlobal()),
                rule.getType() != null ? rule.getType().name() : null,
                rule.getSeverity() != null ? rule.getSeverity().name() : null,
                rule.getMessage(),
                isEnabled
        );
    }
}
