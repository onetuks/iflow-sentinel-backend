package com.onetuks.iflow_sentinel.reprocess.dto;

import java.time.LocalDateTime;

public record MessageReprocessResult(
        String messageId,
        boolean success,
        String statusMessage,
        String storageType,
        String storageName,
        LocalDateTime reprocessedAt,
        String deepLinkUrl
) {
}
