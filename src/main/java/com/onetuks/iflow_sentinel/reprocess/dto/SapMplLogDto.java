package com.onetuks.iflow_sentinel.reprocess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SapMplLogDto(
        @JsonProperty("MessageGuid") String messageGuid,
        @JsonProperty("CorrelationId") String correlationId,
        @JsonProperty("Status") String status,
        @JsonProperty("LogStart") String logStart,
        @JsonProperty("LogEnd") String logEnd,
        @JsonProperty("IntegrationArtifact") IntegrationArtifactRef integrationArtifact,
        @JsonProperty("LogLevel") String logLevel
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IntegrationArtifactRef(
            @JsonProperty("Id") String id,
            @JsonProperty("Name") String name,
            @JsonProperty("Type") String type
    ) {
    }
}
