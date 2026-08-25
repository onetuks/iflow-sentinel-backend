package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantAuthType;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantPlatform;

/** clientSecret 및 interfacePassword는 절대 포함하지 않는다(설계서 11장 민감정보 최소화). */
public record TenantResponse(
        Long id,
        Long projectId,
        String name,
        String odataUrl,
        String tokenUrl,
        TenantPlatform platformType,
        TenantAuthType authType,
        String clientId,
        String interfaceUrl,
        String interfaceTokenUrl,
        TenantAuthType interfaceAuthType,
        String interfaceUsername,
        String status,
        Integer packageCount) {

    public TenantResponse(
            Long id,
            Long projectId,
            String name,
            String odataUrl,
            String tokenUrl,
            TenantPlatform platformType,
            TenantAuthType authType,
            String clientId,
            String status,
            Integer packageCount) {
        this(id, projectId, name, odataUrl, tokenUrl, platformType, authType, clientId, null, null, TenantAuthType.BASIC, null, status, packageCount);
    }

    public TenantResponse(
            Long id,
            Long projectId,
            String name,
            String odataUrl,
            String tokenUrl,
            TenantPlatform platformType,
            TenantAuthType authType,
            String clientId,
            TenantAuthType interfaceAuthType,
            String interfaceUsername,
            String status,
            Integer packageCount) {
        this(id, projectId, name, odataUrl, tokenUrl, platformType, authType, clientId, null, null, interfaceAuthType, interfaceUsername, status, packageCount);
    }

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getProject() != null ? tenant.getProject().getId() : null,
                tenant.getName(),
                tenant.getOdataUrl(),
                tenant.getTokenUrl(),
                tenant.getPlatformType(),
                tenant.getAuthType(),
                tenant.getClientId(),
                tenant.getInterfaceUrl(),
                tenant.getInterfaceTokenUrl(),
                tenant.getInterfaceAuthType(),
                tenant.getInterfaceUsername(),
                "connected",
                0);
    }
}
