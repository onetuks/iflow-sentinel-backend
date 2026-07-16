package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import org.springframework.data.domain.Page;

public record TenantResponse(
    Long id,
    String name,
    String odataUrl,
    String platformType,
    String authType,
    String clientId
) {

  public static TenantResponse from(Tenant entity) {
    return new TenantResponse(
        entity.getId(),
        entity.getName(),
        entity.getOdataUrl(),
        entity.getPlatformType().name(),
        entity.getAuthType().name(),
        entity.getClientId()
    );
  }

  public record TenantResponses(
      Page<TenantResponse> responses
  ) {

    public static TenantResponses from(Page<Tenant> entities) {
      return new TenantResponses(entities.map(TenantResponse::from));
    }
  }
}
