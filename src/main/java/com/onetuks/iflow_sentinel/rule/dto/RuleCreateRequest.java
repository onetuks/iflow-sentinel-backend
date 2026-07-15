package com.onetuks.iflow_sentinel.rule.dto;

import com.onetuks.iflow_sentinel.rule.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.rule.domain.rule.Severity;
import java.util.Map;

public record RuleCreateRequest(
    String ruleKey,
    Boolean isGlobal,
    Long customProjectId,
    RuleType type,
    Severity severity,
    Map<String, Object> target,
    Map<String, Object> params,
    String message,
    boolean enabled
) {}
