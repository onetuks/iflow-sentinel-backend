package com.onetuks.iflow_sentinel.reprocess.dto;

import java.time.LocalDateTime;

public record MessageBodyResponse(
        String messageId,
        String storageType,
        String storageName,
        String messageBody,
        Integer expireDays,
        Integer daysUntilExpiration,
        boolean isExpired,
        LocalDateTime fetchedAt,
        String deepLinkUrl
) {
}
