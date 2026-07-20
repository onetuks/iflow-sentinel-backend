package com.onetuks.iflow_sentinel.rulemgmt.dto;

import com.onetuks.iflow_sentinel.domain.rule.Severity;

public record BindingOverrideRequest(Long ruleId, Severity overriddenSeverity, Boolean overriddenEnabled) {
}
