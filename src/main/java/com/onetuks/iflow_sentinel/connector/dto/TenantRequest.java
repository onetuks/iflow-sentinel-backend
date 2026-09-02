package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantPlatform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TenantRequest(
        Long projectId,
        @NotBlank String name,
        @NotNull TenantPlatform platformType,
        @NotBlank String apiUrl,
        @NotBlank String apiTokenUrl,
        @NotBlank String apiClientId,
        @NotBlank String apiClientSecret,
        LocalDate apiCreateDate,
        String ifUrl,
        String ifTokenUrl,
        String ifClientID,
        String ifClientSecret,
        LocalDate ifCreateDate) {
}
