package com.onetuks.iflow_sentinel.rule.dto;

import java.util.Map;

import com.onetuks.iflow_sentinel.rule.domain.RuleType;
import com.onetuks.iflow_sentinel.rule.domain.Severity;

public record RuleCreateRequest(
                String ruleKey,
                Boolean isGlobal,
                Long customProjectId,
                RuleType type,
                Severity severity,
                Map<String, Object> target,
                Map<String, Object> params,
                String message,
                boolean enabled) {
}
