package com.onetuks.iflow_sentinel.connector.dto;

public record ArtifactConfigurationResponse(
        String name,
        String defaultValue,
        String configuredValue,
        String dataType
) {
}
