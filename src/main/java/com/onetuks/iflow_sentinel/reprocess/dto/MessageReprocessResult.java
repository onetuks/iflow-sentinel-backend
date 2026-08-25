package com.onetuks.iflow_sentinel.reprocess.dto;

import java.time.LocalDateTime;

public record MessageReprocessResult(
        Long historyId,
        String messageId,
        boolean success,
        String statusMessage,
        String storageType,
        String storageName,
        LocalDateTime reprocessedAt,
        String deepLinkUrl,
        String endpointUrl,
        Integer httpStatusCode
) {
    public MessageReprocessResult(
            Long historyId,
            String messageId,
            boolean success,
            String statusMessage,
            String storageType,
            String storageName,
            LocalDateTime reprocessedAt,
            String deepLinkUrl) {
        this(historyId, messageId, success, statusMessage, storageType, storageName, reprocessedAt, deepLinkUrl, null, null);
    }
}
