package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackage;
import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackageRepository;
import com.onetuks.iflow_sentinel.connector.dto.ArtifactResponse;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ArtifactService {

    private final IntegrationPackageRepository packageRepository;
    private final ArtifactRepository artifactRepository;
    private final ArtifactSyncService artifactSyncService;

    public ArtifactService(IntegrationPackageRepository packageRepository, ArtifactRepository artifactRepository,
            ArtifactSyncService artifactSyncService) {
        this.packageRepository = packageRepository;
        this.artifactRepository = artifactRepository;
        this.artifactSyncService = artifactSyncService;
    }

    public List<ArtifactResponse> sync(Long packageId) {
        IntegrationPackage integrationPackage = packageRepository.findById(packageId)
                .orElseThrow(() -> new NoSuchElementException("패키지를 찾을 수 없습니다: " + packageId));
        return artifactSyncService.syncArtifacts(integrationPackage).stream().map(ArtifactResponse::from).toList();
    }

    public List<ArtifactResponse> list(Long packageId) {
        return artifactRepository.findByIntegrationPackageId(packageId).stream().map(ArtifactResponse::from).toList();
    }

    public void delete(Long id) {
        artifactRepository.deleteById(id);
    }
}
