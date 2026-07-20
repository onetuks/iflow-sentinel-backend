package com.onetuks.iflow_sentinel.connector.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** SAP IS IntegrationDesigntimeArtifacts OData 엔티티 중 필요한 필드만. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SapArtifactDto(String Id, String Name, String Version, String PackageId) {
}
