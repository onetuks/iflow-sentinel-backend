package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.domain.tenant.TenantAuthType;
import com.onetuks.iflow_sentinel.domain.tenant.TenantPlatform;

public record TenantUpdateRequest(
    String name,
    String odataUrl,
    TenantPlatform platformType,
    TenantAuthType authType,
    String clientId,
    String clientSecret
) {}
