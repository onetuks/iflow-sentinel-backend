package com.onetuks.iflow_sentinel.domain.artifact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtifactRepository extends JpaRepository<Artifact, Long> {

    List<Artifact> findByIntegrationPackageId(Long integrationPackageId);

    Optional<Artifact> findBySapArtifactId(String sapArtifactId);
}
