package com.onetuks.iflow_sentinel.connector.domain.artifact;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArtifactRepository extends JpaRepository<Artifact, String> {

    List<Artifact> findByIntegrationPackageId(Long integrationPackageId);

    List<Artifact> findByIntegrationPackageTenantId(Long tenantId);

    @Query("SELECT a FROM Artifact a LEFT JOIN FETCH a.integrationPackage p LEFT JOIN FETCH p.tenant WHERE a.sapArtifactId = :id")
    Optional<Artifact> findWithPackageAndTenantById(@Param("id") String id);

    @Query("SELECT a FROM Artifact a LEFT JOIN FETCH a.integrationPackage p LEFT JOIN FETCH p.tenant WHERE p.id = :integrationPackageId")
    List<Artifact> findWithPackageAndTenantByIntegrationPackageId(@Param("integrationPackageId") Long integrationPackageId);

    default Optional<Artifact> findBySapArtifactId(String sapArtifactId) {
        return findById(sapArtifactId);
    }
}
