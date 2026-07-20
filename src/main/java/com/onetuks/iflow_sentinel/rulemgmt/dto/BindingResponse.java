package com.onetuks.iflow_sentinel.rulemgmt.dto;

import com.onetuks.iflow_sentinel.domain.binding.Binding;

public record BindingResponse(Long id, Long projectId, Long rulesetId) {

    public static BindingResponse from(Binding binding) {
        return new BindingResponse(binding.getId(), binding.getProject().getId(), binding.getRuleset().getId());
    }
}
