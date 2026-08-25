package com.onetuks.iflow_sentinel.reprocess.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StorageMappingRepository extends JpaRepository<StorageMapping, Long> {

    List<StorageMapping> findByTenantIdAndArtifactId(Long tenantId, String artifactId);

    Optional<StorageMapping> findByTenantIdAndArtifactIdAndStorageType(Long tenantId, String artifactId, StorageType storageType);

    void deleteByTenantIdAndArtifactId(Long tenantId, String artifactId);
}
