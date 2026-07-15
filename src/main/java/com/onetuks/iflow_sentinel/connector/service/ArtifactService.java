package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackage;
import com.onetuks.iflow_sentinel.connector.dto.ArtifactCreateRequest;
import com.onetuks.iflow_sentinel.connector.dto.ArtifactUpdateRequest;
import com.onetuks.iflow_sentinel.connector.persistence.ArtifactJpaRepository;
import com.onetuks.iflow_sentinel.connector.persistence.IntegrationPackageJpaRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArtifactService {

    private final ArtifactJpaRepository artifactRepository;
    private final IntegrationPackageJpaRepository integrationPackageRepository;

    @Transactional
    public Artifact createArtifact(ArtifactCreateRequest request) {
        IntegrationPackage integrationPackage = integrationPackageRepository.findById(request.integrationPackageId())
                .orElseThrow(NoSuchElementException::new);

        Artifact newArtifact = Artifact.builder()
                .integrationPackage(integrationPackage)
                .sapArtifactId(request.sapArtifactId())
                .name(request.name())
                .version(request.version())
                .type(request.type())
                .build();

        return artifactRepository.save(newArtifact);
    }

    @Transactional
    public Artifact updateArtifact(Long id, ArtifactUpdateRequest request) {
        Artifact artifact = artifactRepository.findById(id).orElseThrow(NoSuchElementException::new);
        // Add update logic here if Entity supports it
        return artifactRepository.save(artifact);
    }

    @Transactional(readOnly = true)
    public Artifact getArtifactById(Long id) {
        return artifactRepository.findById(id).orElseThrow(NoSuchElementException::new);
    }

    @Transactional
    public void removeArtifact(Long id) {
        artifactRepository.deleteById(id);
    }
}
