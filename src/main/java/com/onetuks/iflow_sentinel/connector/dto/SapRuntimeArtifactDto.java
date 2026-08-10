package com.onetuks.iflow_sentinel.connector.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** SAP IS IntegrationRuntimeArtifacts OData 엔티티 중 필요한 필드만. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SapRuntimeArtifactDto(String Id, String Name, String Version, String Type, String Status) {
}
