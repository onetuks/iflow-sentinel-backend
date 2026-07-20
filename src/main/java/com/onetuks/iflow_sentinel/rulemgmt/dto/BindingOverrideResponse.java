package com.onetuks.iflow_sentinel.rulemgmt.dto;

import com.onetuks.iflow_sentinel.domain.binding.BindingOverride;
import com.onetuks.iflow_sentinel.domain.rule.Severity;

public record BindingOverrideResponse(Long id, Long bindingId, Long ruleId, Severity overriddenSeverity, Boolean overriddenEnabled) {

    public static BindingOverrideResponse from(BindingOverride override) {
        return new BindingOverrideResponse(
                override.getId(),
                override.getBinding().getId(),
                override.getRule().getId(),
                override.getOverriddenSeverity(),
                override.getOverriddenEnabled()
        );
    }
}
