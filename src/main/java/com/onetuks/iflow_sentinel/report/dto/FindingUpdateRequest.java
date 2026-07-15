package com.onetuks.iflow_sentinel.report.dto;

import com.onetuks.iflow_sentinel.rule.domain.rule.Severity;

public record FindingUpdateRequest(
    Severity severity,
    String location,
    String message
) {}
