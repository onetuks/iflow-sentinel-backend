package com.onetuks.iflow_sentinel.notification.dto;

import com.onetuks.iflow_sentinel.notification.domain.TenantNotificationConfig;

import java.time.LocalDateTime;

public record TenantNotificationConfigResponse(
        Long id,
        Long tenantId,
        String tenantName,
        boolean isEnabled,
        String recipients,
        int intervalMinutes,
        LocalDateTime lastCheckedAt,
        LocalDateTime lastNotifiedAt
) {
    public static TenantNotificationConfigResponse from(TenantNotificationConfig config) {
        return new TenantNotificationConfigResponse(
                config.getId(),
                config.getTenant().getId(),
                config.getTenant().getName(),
                config.isEnabled(),
                config.getRecipients(),
                config.getIntervalMinutes(),
                config.getLastCheckedAt(),
                config.getLastNotifiedAt()
        );
    }
}

