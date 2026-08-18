package com.onetuks.iflow_sentinel.reprocess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SapMplLogDto(
        @JsonProperty("MessageGuid") String messageGuid,
        @JsonProperty("CorrelationId") String correlationId,
        @JsonProperty("Status") String status,
        @JsonProperty("SubStatus") String subStatus,
        @JsonProperty("CustomStatus") String customStatus,
        @JsonProperty("IntegrationFlowName") String integrationFlowName,
        @JsonProperty("LogStart") String logStart,
        @JsonProperty("LogEnd") String logEnd,
        @JsonProperty("IntegrationArtifact") IntegrationArtifactRef integrationArtifact,
        @JsonProperty("LogLevel") String logLevel,
        @JsonProperty("LastError") String lastError,
        @JsonProperty("ErrorInformation") ErrorInformationRef errorInformation
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IntegrationArtifactRef(
            @JsonProperty("Id") String id,
            @JsonProperty("Name") String name,
            @JsonProperty("Type") String type
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorInformationRef(
            @JsonProperty("LastError") String lastError,
            @JsonProperty("LastErrorModel") String lastErrorModel
    ) {
    }

    public String getArtifactIdOrName() {
        if (integrationArtifact != null) {
            if (integrationArtifact.id() != null && !integrationArtifact.id().isBlank()) {
                return integrationArtifact.id();
            }
            if (integrationArtifact.name() != null && !integrationArtifact.name().isBlank()) {
                return integrationArtifact.name();
            }
        }
        if (integrationFlowName != null && !integrationFlowName.isBlank()) {
            return integrationFlowName;
        }
        return null;
    }

    public String getEffectiveErrorDetail() {
        if (lastError != null && !lastError.isBlank()) {
            return lastError;
        }
        if (errorInformation != null) {
            if (errorInformation.lastError() != null && !errorInformation.lastError().isBlank()) {
                return errorInformation.lastError();
            }
            if (errorInformation.lastErrorModel() != null && !errorInformation.lastErrorModel().isBlank()) {
                return errorInformation.lastErrorModel();
            }
        }
        return null;
    }
}
