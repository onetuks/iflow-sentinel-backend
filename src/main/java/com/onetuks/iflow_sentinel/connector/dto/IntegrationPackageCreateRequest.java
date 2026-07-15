package com.onetuks.iflow_sentinel.connector.dto;

public record IntegrationPackageCreateRequest(
    Long tenantId,
    String sapPackageId,
    String name
) {}
