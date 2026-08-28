package com.onetuks.iflow_sentinel.notification.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TenantNotificationConfigRequest(
        @NotNull(message = "활성화 여부는 필수입니다.")
        Boolean isEnabled,

        String recipients,

        @Min(value = 1, message = "탐색 주기는 최소 1분 이상이어야 합니다.")
        Integer intervalMinutes
) {
}

