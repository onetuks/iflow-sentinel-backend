package com.onetuks.iflow_sentinel.reprocess.dto;

import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessHistory;
import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessStatus;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageType;

import java.time.LocalDateTime;

public record ReprocessHistoryResponse(
        Long id,
        Long tenantId,
        String tenantName,
        String artifactId,
        String artifactName,
        String messageId,
        StorageType storageType,
        String storageName,
        ReprocessStatus status,
        String statusMessage,
        LocalDateTime reprocessedAt,
        String reprocessedBy,
        String deepLinkUrl,
        String endpointUrl,
        Integer httpStatusCode
) {
    public ReprocessHistoryResponse(
            Long id,
            Long tenantId,
            String tenantName,
            String artifactId,
            String artifactName,
            String messageId,
            StorageType storageType,
            String storageName,
            ReprocessStatus status,
            String statusMessage,
            LocalDateTime reprocessedAt,
            String reprocessedBy,
            String deepLinkUrl) {
        this(id, tenantId, tenantName, artifactId, artifactName, messageId, storageType, storageName, status, statusMessage, reprocessedAt, reprocessedBy, deepLinkUrl, null, null);
    }

    public static ReprocessHistoryResponse from(ReprocessHistory history) {
        return new ReprocessHistoryResponse(
                history.getId(),
                history.getTenantId(),
                history.getTenantName(),
                history.getArtifactId(),
                history.getArtifactName(),
                history.getMessageId(),
                history.getStorageType(),
                history.getStorageName(),
                history.getStatus(),
                history.getStatusMessage(),
                history.getReprocessedAt(),
                history.getReprocessedBy(),
                history.getDeepLinkUrl(),
                history.getEndpointUrl(),
                history.getHttpStatusCode()
        );
    }
}
