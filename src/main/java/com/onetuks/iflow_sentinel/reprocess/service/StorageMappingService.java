package com.onetuks.iflow_sentinel.reprocess.service;

import com.onetuks.iflow_sentinel.reprocess.domain.ConfidenceLevel;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageMapping;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageMappingRepository;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageType;
import com.onetuks.iflow_sentinel.reprocess.dto.StorageMappingDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StorageMappingService {

    private final StorageMappingRepository storageMappingRepository;

    public StorageMappingService(StorageMappingRepository storageMappingRepository) {
        this.storageMappingRepository = storageMappingRepository;
    }

    @Transactional(readOnly = true)
    public List<StorageMappingDto> getStorageMappings(Long tenantId, String artifactId) {
        return storageMappingRepository.findByTenantIdAndArtifactId(tenantId, artifactId).stream()
                .map(StorageMappingDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<StorageMappingDto> getStorageMapping(Long tenantId, String artifactId, StorageType storageType) {
        return storageMappingRepository.findByTenantIdAndArtifactIdAndStorageType(tenantId, artifactId, storageType)
                .map(StorageMappingDto::from);
    }

    @Transactional
    public StorageMappingDto saveOrUpdateManualMapping(Long tenantId, String artifactId, StorageType storageType,
                                                       String storageName, Integer expireDays) {
        return saveOrUpdateMapping(tenantId, artifactId, storageType, storageName, expireDays, ConfidenceLevel.MANUAL);
    }

    @Transactional
    public StorageMappingDto saveOrUpdateMapping(Long tenantId, String artifactId, StorageType storageType,
                                                 String storageName, Integer expireDays, ConfidenceLevel confidenceLevel) {
        Optional<StorageMapping> existingOpt = storageMappingRepository
                .findByTenantIdAndArtifactIdAndStorageType(tenantId, artifactId, storageType);

        StorageMapping mapping;
        if (existingOpt.isPresent()) {
            mapping = existingOpt.get();
            // 수동 설정이 되어있고, 입력하려는 정보가 자동추출(AUTO_PARSED/ESTIMATED)이면 덮어쓰지 않음
            if (mapping.getConfidenceLevel() == ConfidenceLevel.MANUAL && confidenceLevel != ConfidenceLevel.MANUAL) {
                return StorageMappingDto.from(mapping);
            }
            mapping.update(storageName, expireDays, confidenceLevel);
        } else {
            mapping = StorageMapping.builder()
                    .tenantId(tenantId)
                    .artifactId(artifactId)
                    .storageType(storageType)
                    .storageName(storageName)
                    .expireDays(expireDays)
                    .confidenceLevel(confidenceLevel)
                    .build();
        }

        StorageMapping saved = storageMappingRepository.save(mapping);
        return StorageMappingDto.from(saved);
    }

    @Transactional
    public void deleteMapping(Long tenantId, String artifactId) {
        storageMappingRepository.deleteByTenantIdAndArtifactId(tenantId, artifactId);
    }
}
