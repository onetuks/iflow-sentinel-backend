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
        @NotBlank String clientSecret,
        String interfaceUrl,
        String interfaceTokenUrl,
        TenantAuthType interfaceAuthType,
        String interfaceUsername,
        String interfacePassword) {

    public TenantRequest(
            Long projectId,
            String name,
            String odataUrl,
            String tokenUrl,
            TenantPlatform platformType,
            TenantAuthType authType,
            String clientId,
            String clientSecret) {
        this(projectId, name, odataUrl, tokenUrl, platformType, authType, clientId, clientSecret, null, null, TenantAuthType.BASIC, null, null);
    }

    public TenantRequest(
            Long projectId,
            String name,
            String odataUrl,
            String tokenUrl,
            TenantPlatform platformType,
            TenantAuthType authType,
            String clientId,
            String clientSecret,
            TenantAuthType interfaceAuthType,
            String interfaceUsername,
            String interfacePassword) {
        this(projectId, name, odataUrl, tokenUrl, platformType, authType, clientId, clientSecret, null, null, interfaceAuthType, interfaceUsername, interfacePassword);
    }
}
