package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactType;
import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackage;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.dto.ODataCollectionResponse;
import com.onetuks.iflow_sentinel.connector.dto.SapArtifactDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ART-002: 패키지 내 iFlow 아티팩트 목록을 OData로 조회해 DB에 upsert한다.
 * IntegrationDesigntimeArtifacts 엔드포인트는 iFlow 전용이므로 type은 IFLOW로 고정한다
 * (다른 아티팩트 타입은 별도 엔드포인트가 필요한 향후 확장 — 설계서 13장).
 */
@Service
public class ArtifactSyncService {

    private static final Logger log = LoggerFactory.getLogger(ArtifactSyncService.class);

    private final SapODataClient odataClient;
    private final ArtifactRepository artifactRepository;

    // 프록시(REQUIRES_NEW 트랜잭션)를 거쳐 자기 자신을 호출하기 위한 self-injection.
    // Spring 컨텍스트 밖에서 생성될 경우(단위 테스트 등) this로 대체되어 프록시 없이 동작한다.
    private ArtifactSyncService self;

    public ArtifactSyncService(SapODataClient odataClient, ArtifactRepository artifactRepository) {
        this.odataClient = odataClient;
        this.artifactRepository = artifactRepository;
        this.self = this;
    }

    @Autowired
    public void setSelf(@Lazy ArtifactSyncService self) {
        this.self = self;
    }

    public List<Artifact> syncArtifacts(IntegrationPackage integrationPackage) {
        String relativePath = "/IntegrationPackages('" + integrationPackage.getSapPackageId()
                + "')/IntegrationDesigntimeArtifacts";
        List<SapArtifactDto> dtos;
        try {
            dtos = odataClient.getCollection(
                    integrationPackage.getTenant(),
                    relativePath,
                    new ParameterizedTypeReference<ODataCollectionResponse<SapArtifactDto>>() {
                    });
        } catch (Exception e) {
            // SAP측 응답 지연(504 등)으로 특정 패키지의 아티팩트 목록 조회가 실패해도
            // 테넌트 동기화 전체가 중단되지 않도록 이 패키지만 skip하고 기존 DB 데이터를 그대로 유지한다.
            log.warn("패키지 아티팩트 목록 조회 실패 (skip, 기존 데이터 유지): tenantId={}, packageId={}, message={}",
                    integrationPackage.getTenant().getId(), integrationPackage.getSapPackageId(), e.getMessage());
            return artifactRepository.findByIntegrationPackageId(integrationPackage.getId());
        }

        List<Artifact> result = new ArrayList<>();
        for (SapArtifactDto dto : dtos) {
            try {
                result.add(self.upsertArtifact(integrationPackage, dto));
            } catch (Exception e) {
                // 동일 아티팩트ID가 중복 수신되는 등 SAP측 데이터 이상으로 upsert가 실패해도
                // 테넌트 동기화 전체가 중단되지 않도록 개별 아티팩트 단위로 격리해 skip한다.
                log.warn("아티팩트 동기화 실패 (skip): tenantId={}, packageId={}, artifactId={}, message={}",
                        integrationPackage.getTenant().getId(), integrationPackage.getSapPackageId(), dto.Id(),
                        e.getMessage());
            }
        }
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Artifact upsertArtifact(IntegrationPackage integrationPackage, SapArtifactDto dto) {
        Artifact existing = artifactRepository.findBySapArtifactId(dto.Id()).orElse(null);
        if (existing != null) {
            existing.updateFrom(integrationPackage, dto.Name(), dto.Version(), ArtifactType.IFLOW);
            return artifactRepository.save(existing);
        }
        Artifact created = Artifact.builder()
                .integrationPackage(integrationPackage)
                .sapArtifactId(dto.Id())
                .name(dto.Name())
                .version(dto.Version())
                .type(ArtifactType.IFLOW)
                .build();
        return artifactRepository.save(created);
    }

    public void cleanOrphanArtifacts(Tenant tenant, Set<String> activeSapArtifactIds) {
        List<Artifact> existingArtifacts = artifactRepository.findByIntegrationPackageTenantId(tenant.getId());
        List<Artifact> orphans = existingArtifacts.stream()
                .filter(art -> !activeSapArtifactIds.contains(art.getSapArtifactId()))
                .toList();

        if (!orphans.isEmpty()) {
            artifactRepository.deleteAll(orphans);
        }
    }
}
