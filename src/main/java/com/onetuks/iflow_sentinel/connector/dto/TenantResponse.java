package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.domain.tenant.TenantAuthType;
import com.onetuks.iflow_sentinel.domain.tenant.TenantPlatform;

/** clientSecret은 절대 포함하지 않는다(설계서 11장 민감정보 최소화). */
public record TenantResponse(
        Long id,
        Long projectId,
        String name,
        String odataUrl,
        String tokenUrl,
        TenantPlatform platformType,
        TenantAuthType authType,
        String clientId
) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getProject().getId(),
                tenant.getName(),
                tenant.getOdataUrl(),
                tenant.getTokenUrl(),
                tenant.getPlatformType(),
                tenant.getAuthType(),
                tenant.getClientId()
        );
    }
}
