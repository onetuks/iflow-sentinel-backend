package com.onetuks.iflow_sentinel.connector.dto;

import com.onetuks.iflow_sentinel.connector.domain.tenant.LogLevel;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantLogLevelSetting;

public record TenantLogLevelResponse(Long tenantId, LogLevel logLevel) {

    public static TenantLogLevelResponse from(TenantLogLevelSetting setting) {
        return new TenantLogLevelResponse(setting.getTenant().getId(), setting.getLogLevel());
    }
}
