package com.onetuks.iflow_sentinel.rulemgmt.dto;

import com.onetuks.iflow_sentinel.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.domain.rule.Severity;

import java.util.Map;

public record RuleRequest(
        String ruleKey,
        RuleType type,
        Severity severity,
        Map<String, Object> target,
        Map<String, Object> params,
        String message,
        boolean enabled
) {
}
