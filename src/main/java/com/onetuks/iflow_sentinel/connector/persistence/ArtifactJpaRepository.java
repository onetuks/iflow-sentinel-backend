package com.onetuks.iflow_sentinel.connector.persistence;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtifactJpaRepository extends JpaRepository<Artifact, Long> {
}
