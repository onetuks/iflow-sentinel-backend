package com.onetuks.iflow_sentinel.reprocess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * SAP CPI OData /ServiceEndpoints 엔티티 매핑 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SapServiceEndpointDto(
        String Name,
        String Url,
        String Protocol
) {
}
