package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.connector.dto.ArtifactDeploymentStatus;
import com.onetuks.iflow_sentinel.connector.dto.ODataCollectionResponse;
import com.onetuks.iflow_sentinel.connector.dto.SapArtifactDto;
import com.onetuks.iflow_sentinel.connector.dto.SapPackageDto;
import com.onetuks.iflow_sentinel.connector.dto.SapRuntimeArtifactDto;
import com.onetuks.iflow_sentinel.connector.dto.TrackerArtifactResponse;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * SAP를 실시간으로 조회해 design-time/runtime 아티팩트를 ID 기준으로 병합, 배포 상태를 분류한다.
 * 로컬 DB에 결과를 저장하지 않는 읽기 전용 조회이며, 룰엔진 검사용 스냅샷을 만드는
 * {@link PackageSyncService}/{@link ArtifactSyncService}와는 별개의 목적을 가진다.
 */
@Service
public class ArtifactTrackerService {

    private final TenantRepository tenantRepository;
    private final SapODataClient odataClient;

    public ArtifactTrackerService(TenantRepository tenantRepository, SapODataClient odataClient) {
        this.tenantRepository = tenantRepository;
        this.odataClient = odataClient;
    }

    public List<TrackerArtifactResponse> list(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("테넌트를 찾을 수 없습니다: " + tenantId));

        List<SapPackageDto> packages = odataClient.getCollection(
                tenant,
                "/IntegrationPackages",
                new ParameterizedTypeReference<ODataCollectionResponse<SapPackageDto>>() {
                });

        Map<String, SapRuntimeArtifactDto> runtimeById = new HashMap<>();
        for (SapRuntimeArtifactDto dto : odataClient.getCollection(
                tenant,
                "/IntegrationRuntimeArtifacts",
                new ParameterizedTypeReference<ODataCollectionResponse<SapRuntimeArtifactDto>>() {
                })) {
            runtimeById.put(dto.Id(), dto);
        }

        List<TrackerArtifactResponse> result = new ArrayList<>();
        Set<String> matchedRuntimeIds = new HashSet<>();

        for (SapPackageDto pkg : packages) {
            List<SapArtifactDto> artifacts = odataClient.getCollection(
                    tenant,
                    "/IntegrationPackages('" + pkg.Id() + "')/IntegrationDesigntimeArtifacts",
                    new ParameterizedTypeReference<ODataCollectionResponse<SapArtifactDto>>() {
                    });
            for (SapArtifactDto artifact : artifacts) {
                SapRuntimeArtifactDto runtime = runtimeById.get(artifact.Id());
                if (runtime != null) {
                    matchedRuntimeIds.add(artifact.Id());
                }
                result.add(new TrackerArtifactResponse(
                        pkg.Id(),
                        pkg.Name(),
                        artifact.Id(),
                        artifact.Name(),
                        artifact.Version(),
                        runtime == null ? null : runtime.Status(),
                        runtime == null ? ArtifactDeploymentStatus.NOT_DEPLOYED : ArtifactDeploymentStatus.DEPLOYED));
            }
        }

        for (SapRuntimeArtifactDto runtime : runtimeById.values()) {
            if (matchedRuntimeIds.contains(runtime.Id())) {
                continue;
            }
            result.add(new TrackerArtifactResponse(
                    null,
                    null,
                    runtime.Id(),
                    runtime.Name(),
                    runtime.Version(),
                    runtime.Status(),
                    ArtifactDeploymentStatus.INACTIVE));
        }

        return result;
    }
}
