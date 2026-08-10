package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantAuthType;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantPlatform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TenantRequest(
        Long projectId,
        @NotBlank String name,
        @NotBlank String odataUrl,
        @NotBlank String tokenUrl,
        @NotNull TenantPlatform platformType,
        @NotNull TenantAuthType authType,
        @NotBlank String clientId,
        @NotBlank String clientSecret) {
}
