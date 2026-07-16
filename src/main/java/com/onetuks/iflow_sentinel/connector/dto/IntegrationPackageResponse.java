package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackage;
import org.springframework.data.domain.Page;

public record IntegrationPackageResponse(
    Long id,
    String sapPackageId,
    String name
) {

  public static IntegrationPackageResponse from(IntegrationPackage entity) {
    return new IntegrationPackageResponse(
        entity.getId(),
        entity.getSapPackageId(),
        entity.getName()
    );
  }

  public record IntegrationPackageResponses(
      Page<IntegrationPackageResponse> responses
  ) {

    public static IntegrationPackageResponses from(Page<IntegrationPackage> entities) {
      return new IntegrationPackageResponses(entities.map(IntegrationPackageResponse::from));
    }
  }
}
