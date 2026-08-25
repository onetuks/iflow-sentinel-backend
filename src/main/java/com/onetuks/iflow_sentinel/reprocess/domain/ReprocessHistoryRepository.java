package com.onetuks.iflow_sentinel.reprocess.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReprocessHistoryRepository extends JpaRepository<ReprocessHistory, Long> {

    List<ReprocessHistory> findByTenantIdOrderByReprocessedAtDesc(Long tenantId);

    List<ReprocessHistory> findByTenantIdAndArtifactIdOrderByReprocessedAtDesc(Long tenantId, String artifactId);

    List<ReprocessHistory> findByMessageIdOrderByReprocessedAtDesc(String messageId);

    @Query("SELECT h FROM ReprocessHistory h " +
            "WHERE (:tenantId IS NULL OR h.tenantId = :tenantId) " +
            "AND (:artifactId IS NULL OR h.artifactId = :artifactId) " +
            "AND (:messageId IS NULL OR h.messageId LIKE CONCAT('%', :messageId, '%')) " +
            "AND (:status IS NULL OR h.status = :status) " +
            "ORDER BY h.reprocessedAt DESC")
    List<ReprocessHistory> searchHistories(
            @Param("tenantId") Long tenantId,
            @Param("artifactId") String artifactId,
            @Param("messageId") String messageId,
            @Param("status") ReprocessStatus status
    );
}
