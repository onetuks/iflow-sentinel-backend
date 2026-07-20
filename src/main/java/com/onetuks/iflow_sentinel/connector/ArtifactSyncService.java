package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.dto.ODataCollectionResponse;
import com.onetuks.iflow_sentinel.connector.dto.SapArtifactDto;
import com.onetuks.iflow_sentinel.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.domain.artifact.ArtifactType;
import com.onetuks.iflow_sentinel.domain.integrationpackage.IntegrationPackage;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ART-002: 패키지 내 iFlow 아티팩트 목록을 OData로 조회해 DB에 upsert한다.
 * IntegrationDesigntimeArtifacts 엔드포인트는 iFlow 전용이므로 type은 IFLOW로 고정한다
 * (다른 아티팩트 타입은 별도 엔드포인트가 필요한 향후 확장 — 설계서 13장).
 */
@Service
public class ArtifactSyncService {

    private final SapODataClient odataClient;
    private final ArtifactRepository artifactRepository;

    public ArtifactSyncService(SapODataClient odataClient, ArtifactRepository artifactRepository) {
        this.odataClient = odataClient;
        this.artifactRepository = artifactRepository;
    }

    public List<Artifact> syncArtifacts(IntegrationPackage integrationPackage) {
        String relativePath = "/IntegrationPackages('" + integrationPackage.getSapPackageId() + "')/IntegrationDesigntimeArtifacts";
        List<SapArtifactDto> dtos = odataClient.getCollection(
                integrationPackage.getTenant(),
                relativePath,
                new ParameterizedTypeReference<ODataCollectionResponse<SapArtifactDto>>() {
                }
        );

        List<Artifact> result = new ArrayList<>();
        for (SapArtifactDto dto : dtos) {
            Artifact existing = artifactRepository.findBySapArtifactId(dto.Id()).orElse(null);
            if (existing != null) {
                existing.updateFrom(dto.Name(), dto.Version(), ArtifactType.IFLOW);
                result.add(artifactRepository.save(existing));
            } else {
                Artifact created = Artifact.builder()
                        .integrationPackage(integrationPackage)
                        .sapArtifactId(dto.Id())
                        .name(dto.Name())
                        .version(dto.Version())
                        .type(ArtifactType.IFLOW)
                        .build();
                result.add(artifactRepository.save(created));
            }
        }
        return result;
    }
}
