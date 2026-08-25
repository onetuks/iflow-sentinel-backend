package com.onetuks.iflow_sentinel.reprocess.dto;

import com.onetuks.iflow_sentinel.reprocess.domain.StorageType;

public record MessageReprocessRequest(
        Long tenantId,
        String artifactId,
        String messageId,
        StorageType storageType,
        String storageName,
        String reprocessedBy,
        String payload,
        String endpointUrl
) {
    public MessageReprocessRequest(
            Long tenantId,
            String artifactId,
            String messageId,
            StorageType storageType,
            String storageName,
            String reprocessedBy) {
        this(tenantId, artifactId, messageId, storageType, storageName, reprocessedBy, null, null);
    }
}
