package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantPlatform;

import java.time.LocalDate;

/** apiClientSecret 및 ifClientSecret은 절대 포함하지 않는다(설계서 11장 민감정보 최소화). */
public record TenantResponse(
        Long id,
        Long projectId,
        String name,
        TenantPlatform platformType,
        String apiUrl,
        String apiTokenUrl,
        String apiClientId,
        LocalDate apiCreateDate,
        String ifUrl,
        String ifTokenUrl,
        String ifClientID,
        LocalDate ifCreateDate,
        String status,
        Integer packageCount) {

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getProject() != null ? tenant.getProject().getId() : null,
                tenant.getName(),
                tenant.getPlatformType(),
                tenant.getApiUrl(),
                tenant.getApiTokenUrl(),
                tenant.getApiClientId(),
                tenant.getApiCreateDate(),
                tenant.getIfUrl(),
                tenant.getIfTokenUrl(),
                tenant.getIfClientID(),
                tenant.getIfCreateDate(),
                "connected",
                0);
    }
}
