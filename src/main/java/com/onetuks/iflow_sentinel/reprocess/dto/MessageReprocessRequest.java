package com.onetuks.iflow_sentinel.reprocess.dto;

import com.onetuks.iflow_sentinel.reprocess.domain.ProtocolType;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageType;

public record MessageReprocessRequest(
        Long tenantId,
        String artifactId,
        String messageId,
        StorageType storageType,
        String storageName,
        String reprocessedBy,
        String payload,
        String endpointUrl,
        ProtocolType protocolType,
        String soapAction
) {
    public MessageReprocessRequest(
            Long tenantId,
            String artifactId,
            String messageId,
            StorageType storageType,
            String storageName,
            String reprocessedBy) {
        this(tenantId, artifactId, messageId, storageType, storageName, reprocessedBy, null, null, null, null);
    }

    public MessageReprocessRequest(
            Long tenantId,
            String artifactId,
            String messageId,
            StorageType storageType,
            String storageName,
            String reprocessedBy,
            String payload,
            String endpointUrl) {
        this(tenantId, artifactId, messageId, storageType, storageName, reprocessedBy, payload, endpointUrl, null, null);
    }
}
