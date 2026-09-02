package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.connector.dto.ArtifactConfigurationResponse;
import com.onetuks.iflow_sentinel.connector.dto.ArtifactDeploymentStatus;
import com.onetuks.iflow_sentinel.connector.dto.ODataCollectionResponse;
import com.onetuks.iflow_sentinel.connector.dto.SapArtifactDto;
import com.onetuks.iflow_sentinel.connector.dto.SapConfigurationDto;
import com.onetuks.iflow_sentinel.connector.dto.SapPackageDto;
import com.onetuks.iflow_sentinel.connector.dto.SapRuntimeArtifactDto;
import com.onetuks.iflow_sentinel.connector.dto.TrackerArtifactResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final Logger log = LoggerFactory.getLogger(ArtifactTrackerService.class);

    private final TenantRepository tenantRepository;
    private final SapODataClient odataClient;
    private final ArtifactRepository artifactRepository;
    private final PackageDefaultValueService packageDefaultValueService;

    public ArtifactTrackerService(
            TenantRepository tenantRepository,
            SapODataClient odataClient,
            ArtifactRepository artifactRepository,
            PackageDefaultValueService packageDefaultValueService) {
        this.tenantRepository = tenantRepository;
        this.odataClient = odataClient;
        this.artifactRepository = artifactRepository;
        this.packageDefaultValueService = packageDefaultValueService;
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

    // getDefaultValues()가 캐시 미스 시 내부적으로 DB 쓰기(syncPackageDefaultValues)를 수행할 수 있으므로
    // readOnly로 지정하면 안 된다 (readOnly 트랜잭션에 join되면 그 쓰기가 거부될 수 있다).
    @Transactional
    public List<ArtifactConfigurationResponse> getConfigurations(Long tenantId, String artifactId, String version) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("테넌트를 찾을 수 없습니다: " + tenantId));

        String reqVersion = (version == null || version.isBlank() || "-".equals(version)) ? "active" : version.trim();

        // 1. OData /Configurations 로부터 Configured Value (테넌트 설정값) 조회
        ConfigurationsFetchResult fetchResult = fetchConfigurationsWithFallback(tenant, artifactId, reqVersion);
        List<SapConfigurationDto> odataConfigurations = fetchResult.configurations();
        String effectiveVersion = fetchResult.effectiveVersion();

        // 2. packageId 수집 (ArtifactRepository 혹은 Tracker list 기반)
        String packageId = findPackageIdForArtifact(tenant, artifactId);

        // 3. PackageDefaultValueService를 통해 DB에 저장/동기화된 Default Value 조회
        Map<String, String> defaultValues = packageDefaultValueService.getDefaultValues(
                tenant, packageId, artifactId, effectiveVersion);

        return odataConfigurations.stream()
                .map(dto -> {
                    String name = dto.parameterKey() == null ? null : dto.parameterKey().trim();
                    String configuredVal = dto.parameterValue() == null ? "-" : dto.parameterValue();
                    String defaultVal = defaultValues.getOrDefault(name, "-");
                    return new ArtifactConfigurationResponse(name, defaultVal, configuredVal, dto.dataType());
                })
                .toList();
    }

    private String findPackageIdForArtifact(Tenant tenant, String artifactId) {
        return artifactRepository.findBySapArtifactId(artifactId)
                .map(Artifact::getIntegrationPackage)
                .map(pkg -> pkg.getSapPackageId())
                .orElseGet(() -> {
                    // DB에 없는 경우 실시간 tracker list에서 packageId 탐색
                    try {
                        List<TrackerArtifactResponse> trackerList = list(tenant.getId());
                        return trackerList.stream()
                                .filter(item -> artifactId.equals(item.artifactId()))
                                .map(TrackerArtifactResponse::packageId)
                                .findFirst()
                                .orElse(null);
                    } catch (Exception e) {
                        log.warn("Package ID 찾기 실패 - artifactId: {}", artifactId, e);
                        return null;
                    }
                });
    }

    private ConfigurationsFetchResult fetchConfigurationsWithFallback(Tenant tenant, String artifactId,
            String reqVersion) {
        String relativePath = String.format("/IntegrationDesigntimeArtifacts(Id='%s',Version='%s')/Configurations",
                artifactId, reqVersion);
        try {
            List<SapConfigurationDto> configurations = odataClient.getCollection(
                    tenant,
                    relativePath,
                    new ParameterizedTypeReference<ODataCollectionResponse<SapConfigurationDto>>() {
                    });
            return new ConfigurationsFetchResult(configurations, reqVersion);
        } catch (Exception e) {
            log.warn("OData Configurations fetch with version '{}' failed for artifact {}, fallback to 'active': {}",
                    reqVersion, artifactId, e.getMessage());
            String fallbackPath = String
                    .format("/IntegrationDesigntimeArtifacts(Id='%s',Version='active')/Configurations", artifactId);
            List<SapConfigurationDto> configurations = odataClient.getCollection(
                    tenant,
                    fallbackPath,
                    new ParameterizedTypeReference<ODataCollectionResponse<SapConfigurationDto>>() {
                    });
            return new ConfigurationsFetchResult(configurations, "active");
        }
    }

    private record ConfigurationsFetchResult(List<SapConfigurationDto> configurations, String effectiveVersion) {
    }
}

