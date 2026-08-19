package com.onetuks.iflow_sentinel.reprocess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SAP CPI DataStoreEntries OData V2 JSON 응답 엔티티 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SapDataStoreEntryDto(
        @JsonProperty("Id") String id,
        @JsonProperty("DataStoreName") String dataStoreName,
        @JsonProperty("IntegrationFlow") String integrationFlow,
        @JsonProperty("Type") String type,
        @JsonProperty("Status") String status,
        @JsonProperty("MessageId") String messageId
) {
}
