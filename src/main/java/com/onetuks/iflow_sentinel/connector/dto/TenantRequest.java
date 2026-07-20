package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantAuthType;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantPlatform;

public record TenantRequest(
                Long projectId,
                String name,
                String odataUrl,
                String tokenUrl,
                TenantPlatform platformType,
                TenantAuthType authType,
                String clientId,
                String clientSecret) {
}
