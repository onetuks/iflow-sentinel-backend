package com.onetuks.iflow_sentinel.reprocess.dto;

import java.time.LocalDateTime;

public record MplFailureResponse(
        String messageId,
        String correlationId,
        String status,
        String artifactId,
        String artifactName,
        LocalDateTime logStart,
        LocalDateTime logEnd,
        String storageName,
        String storageType,
        String expirationStatus,
        Integer daysUntilExpiration,
        String errorDetail
) {
}
