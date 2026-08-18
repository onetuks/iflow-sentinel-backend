package com.onetuks.iflow_sentinel.connector.domain.artifact;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtifactDefaultPropertyRepository extends JpaRepository<ArtifactDefaultProperty, Long> {

    List<ArtifactDefaultProperty> findBySapArtifactIdAndVersion(String sapArtifactId, String version);

    Optional<ArtifactDefaultProperty> findBySapArtifactIdAndVersionAndParameterKey(
            String sapArtifactId, String version, String parameterKey);

    void deleteBySapArtifactIdAndVersion(String sapArtifactId, String version);
}
