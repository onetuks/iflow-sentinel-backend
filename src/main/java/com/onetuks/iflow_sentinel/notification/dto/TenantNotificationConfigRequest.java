package com.onetuks.iflow_sentinel.notification.dto;

import jakarta.validation.constraints.NotNull;

public record TenantNotificationConfigRequest(
        @NotNull(message = "활성화 여부는 필수입니다.")
        Boolean isEnabled,

        String recipients
) {
}
