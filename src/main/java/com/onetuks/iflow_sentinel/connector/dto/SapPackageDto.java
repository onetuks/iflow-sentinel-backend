package com.onetuks.iflow_sentinel.connector.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** SAP IS IntegrationPackages OData 엔티티 중 필요한 필드만. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SapPackageDto(String Id, String Name, String Mode) {
}
