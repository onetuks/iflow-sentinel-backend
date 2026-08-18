package com.onetuks.iflow_sentinel.reprocess.dto;

import com.onetuks.iflow_sentinel.reprocess.domain.StorageType;

public record MessageReprocessRequest(
        Long tenantId,
        Long artifactId,
        String messageId,
        StorageType storageType,
        String storageName
) {
}
