package com.onetuks.iflow_sentinel.reprocess.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tenant_artifact_storage_mapping",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tenant_artifact_storage",
                        columnNames = {"tenant_id", "artifact_id", "storage_type"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorageMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "artifact_id", nullable = false)
    private Long artifactId;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false)
    private StorageType storageType;

    @Column(name = "storage_name", nullable = false)
    private String storageName;

    @Column(name = "expire_days")
    private Integer expireDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", nullable = false)
    private ConfidenceLevel confidenceLevel;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public StorageMapping(Long tenantId, Long artifactId, StorageType storageType, String storageName,
                          Integer expireDays, ConfidenceLevel confidenceLevel, LocalDateTime updatedAt) {
        this.tenantId = tenantId;
        this.artifactId = artifactId;
        this.storageType = storageType;
        this.storageName = storageName;
        this.expireDays = expireDays;
        this.confidenceLevel = confidenceLevel;
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    public void update(String storageName, Integer expireDays, ConfidenceLevel confidenceLevel) {
        this.storageName = storageName;
        if (expireDays != null) {
            this.expireDays = expireDays;
        }
        this.confidenceLevel = confidenceLevel;
        this.updatedAt = LocalDateTime.now();
    }
}
