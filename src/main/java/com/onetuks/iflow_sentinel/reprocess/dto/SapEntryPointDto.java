package com.onetuks.iflow_sentinel.reprocess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * SAP CPI OData ServiceEndpoints('{id}')/EntryPoints 엔티티 매핑 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SapEntryPointDto(
        String Url,
        String ContentType,
        String HttpMethod
) {
}
