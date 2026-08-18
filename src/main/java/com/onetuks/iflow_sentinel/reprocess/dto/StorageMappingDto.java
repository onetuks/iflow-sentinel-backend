package com.onetuks.iflow_sentinel.reprocess.dto;

import com.onetuks.iflow_sentinel.reprocess.domain.ConfidenceLevel;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageMapping;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageType;

import java.time.LocalDateTime;

public record StorageMappingDto(
        Long id,
        Long tenantId,
        Long artifactId,
        StorageType storageType,
        String storageName,
        Integer expireDays,
        ConfidenceLevel confidenceLevel,
        LocalDateTime updatedAt
) {
    public static StorageMappingDto from(StorageMapping mapping) {
        return new StorageMappingDto(
                mapping.getId(),
                mapping.getTenantId(),
                mapping.getArtifactId(),
                mapping.getStorageType(),
                mapping.getStorageName(),
                mapping.getExpireDays(),
                mapping.getConfidenceLevel(),
                mapping.getUpdatedAt()
        );
    }
}
