package com.onetuks.iflow_sentinel.reprocess.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reprocess_history",
        indexes = {
                @Index(name = "idx_reprocess_history_tenant", columnList = "tenant_id"),
                @Index(name = "idx_reprocess_history_artifact", columnList = "artifact_id"),
                @Index(name = "idx_reprocess_history_message", columnList = "message_id"),
                @Index(name = "idx_reprocess_history_time", columnList = "reprocessed_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReprocessHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "tenant_name")
    private String tenantName;

    @Column(name = "artifact_id")
    private String artifactId;

    @Column(name = "artifact_name")
    private String artifactName;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false)
    private StorageType storageType;

    @Column(name = "storage_name")
    private String storageName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReprocessStatus status;

    @Column(name = "status_message", columnDefinition = "TEXT")
    private String statusMessage;

    @Column(name = "reprocessed_at", nullable = false)
    private LocalDateTime reprocessedAt;

    @Column(name = "reprocessed_by")
    private String reprocessedBy;

    @Column(name = "deep_link_url", length = 1000)
    private String deepLinkUrl;

    @Column(name = "endpoint_url", length = 1000)
    private String endpointUrl;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Builder
    public ReprocessHistory(Long tenantId, String tenantName, String artifactId, String artifactName,
                            String messageId, StorageType storageType, String storageName,
                            ReprocessStatus status, String statusMessage, LocalDateTime reprocessedAt,
                            String reprocessedBy, String deepLinkUrl, String endpointUrl, Integer httpStatusCode) {
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.artifactId = artifactId;
        this.artifactName = artifactName;
        this.messageId = messageId;
        this.storageType = storageType;
        this.storageName = storageName;
        this.status = status;
        this.statusMessage = statusMessage;
        this.reprocessedAt = reprocessedAt != null ? reprocessedAt : LocalDateTime.now();
        this.reprocessedBy = reprocessedBy != null ? reprocessedBy : "SYSTEM";
        this.deepLinkUrl = deepLinkUrl;
        this.endpointUrl = endpointUrl;
        this.httpStatusCode = httpStatusCode;
    }
}
