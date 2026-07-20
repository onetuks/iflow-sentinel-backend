package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.domain.integrationpackage.IntegrationPackage;

public record IntegrationPackageResponse(Long id, Long tenantId, String sapPackageId, String name) {

    public static IntegrationPackageResponse from(IntegrationPackage pkg) {
        return new IntegrationPackageResponse(pkg.getId(), pkg.getTenant().getId(), pkg.getSapPackageId(), pkg.getName());
    }
}
