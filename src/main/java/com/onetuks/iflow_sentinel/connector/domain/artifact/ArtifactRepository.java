package com.onetuks.iflow_sentinel.connector.domain.artifact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtifactRepository extends JpaRepository<Artifact, Long> {

    List<Artifact> findByIntegrationPackageId(Long integrationPackageId);

    List<Artifact> findByIntegrationPackageTenantId(Long tenantId);

    Optional<Artifact> findBySapArtifactId(String sapArtifactId);
}
