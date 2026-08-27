package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.connector.domain.tenant.LogLevel;
import jakarta.validation.constraints.NotNull;

public record TenantLogLevelRequest(@NotNull LogLevel logLevel) {
}
