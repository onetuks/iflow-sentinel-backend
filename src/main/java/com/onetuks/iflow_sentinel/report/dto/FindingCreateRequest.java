package com.onetuks.iflow_sentinel.report.dto;

import com.onetuks.iflow_sentinel.rule.domain.rule.Severity;

public record FindingCreateRequest(
    Long checkRunId,
    Long artifactId,
    Long ruleId,
    Severity severity,
    String location,
    String message
) {}
