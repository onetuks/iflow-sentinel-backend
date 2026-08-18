package com.onetuks.iflow_sentinel.connector.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SapConfigurationDto(
        @JsonProperty("ParameterKey") String parameterKey,
        @JsonProperty("ParameterValue") String parameterValue,
        @JsonProperty("DataType") String dataType
) {
}
