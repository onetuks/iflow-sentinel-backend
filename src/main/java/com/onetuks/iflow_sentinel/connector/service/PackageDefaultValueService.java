package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.component.PackageZipParser;
import com.onetuks.iflow_sentinel.connector.component.PackageZipParser.ParsedArtifactDefaultProperties;
import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactDefaultProperty;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactDefaultPropertyRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PackageDefaultValueService {

    private static final Logger log = LoggerFactory.getLogger(PackageDefaultValueService.class);

    private final SapODataClient odataClient;
    private final PackageZipParser packageZipParser;
    private final ArtifactDefaultPropertyRepository defaultPropertyRepository;

    public PackageDefaultValueService(
            SapODataClient odataClient,
            PackageZipParser packageZipParser,
            ArtifactDefaultPropertyRepository defaultPropertyRepository) {
        this.odataClient = odataClient;
        this.packageZipParser = packageZipParser;
        this.defaultPropertyRepository = defaultPropertyRepository;
    }

    /**
     * 아티팩트의 Default Value 조회 (DB에 존재하는 경우 DB 데이터 반환, 미존재 시 패키지 Export ZIP 파싱 후 DB 갱신)
     */
    @Transactional
    public Map<String, String> getDefaultValues(Tenant tenant, String packageId, String artifactId, String version) {
        if (artifactId == null || artifactId.isBlank()) {
            return Map.of();
        }

        String reqVersion = (version == null || version.isBlank() || "-".equals(version)) ? "1.0.0" : version.trim();

        // 1. DB 조회
        List<ArtifactDefaultProperty> existingList =
                defaultPropertyRepository.findBySapArtifactIdAndVersion(artifactId, reqVersion);

        if (!existingList.isEmpty()) {
            log.debug("Default Value DB HIT - artifactId: {}, version: {}", artifactId, reqVersion);
            return existingList.stream()
                    .collect(Collectors.toMap(
                            ArtifactDefaultProperty::getParameterKey,
                            ArtifactDefaultProperty::getDefaultValue,
                            (v1, v2) -> v1
                    ));
        }

        // 2. DB MISS -> 패키지 Export ZIP 1회 다운로드 및 DB 일괄 동기화
        log.info("Default Value DB MISS - artifactId: {}, version: {}. Synchronizing from package: {}",
                artifactId, reqVersion, packageId);
        
        if (packageId == null || packageId.isBlank()) {
            log.warn("packageId가 없어 패키지 Export ZIP을 다운로드할 수 없습니다.");
            return Map.of();
        }

        syncPackageDefaultValues(tenant, packageId);

        // 3. 동기화 후 DB 재조회
        List<ArtifactDefaultProperty> updatedList =
                defaultPropertyRepository.findBySapArtifactIdAndVersion(artifactId, reqVersion);

        if (updatedList.isEmpty()) {
            // 버전 차이 등으로 못 찾았을 경우 version 없이 혹은 재조회 fallback
            log.warn("패키지 동기화 후에도 artifactId: {}, version: {} 에 대한 기본값을 찾지 못했습니다.", artifactId, reqVersion);
            return Map.of();
        }

        return updatedList.stream()
                .collect(Collectors.toMap(
                        ArtifactDefaultProperty::getParameterKey,
                        ArtifactDefaultProperty::getDefaultValue,
                        (v1, v2) -> v1
                ));
    }

    /**
     * 패키지 Export ZIP API를 호출하여 패키지 내 모든 아티팩트의 Default Value를 DB에 저장/갱신
     */
    @Transactional
    public synchronized void syncPackageDefaultValues(Tenant tenant, String packageId) {
        String reqPath = String.format("/IntegrationPackages('%s')/$value", packageId);
        byte[] packageZipBytes;
        try {
            packageZipBytes = odataClient.getBinary(tenant, reqPath);
        } catch (Exception e) {
            log.error("패키지 Export ZIP 다운로드 실패 - packageId: {}, error: {}", packageId, e.getMessage());
            return;
        }

        if (packageZipBytes == null || packageZipBytes.length == 0) {
            log.warn("다운로드된 패키지 Export ZIP 바이너리가 비어있습니다. packageId: {}", packageId);
            return;
        }

        Map<String, ParsedArtifactDefaultProperties> parsedMap = packageZipParser.parsePackageZip(packageZipBytes);

        // parsedMap에 uniqueId와 id 듀얼 키로 들어간 동일 인스턴스 중복 제거
        Set<ParsedArtifactDefaultProperties> uniqueArtifactProps = new HashSet<>(parsedMap.values());

        Map<String, ArtifactDefaultProperty> propertyMapToSave = new HashMap<>();

        for (ParsedArtifactDefaultProperties artifactProps : uniqueArtifactProps) {
            String targetArtifactId = artifactProps.name(); // uniqueId or name
            String targetVersion = artifactProps.version();

            // 기존 DB 데이터 삭제 후 재생성 (Upsert 효과)
            defaultPropertyRepository.deleteBySapArtifactIdAndVersion(targetArtifactId, targetVersion);

            for (Map.Entry<String, String> propEntry : artifactProps.defaultValues().entrySet()) {
                String key = propEntry.getKey();
                String val = propEntry.getValue();
                String compositeKey = targetArtifactId + ":" + targetVersion + ":" + key;

                propertyMapToSave.put(compositeKey, ArtifactDefaultProperty.builder()
                        .sapArtifactId(targetArtifactId)
                        .version(targetVersion)
                        .parameterKey(key)
                        .defaultValue(val)
                        .build());
            }
        }

        if (!propertyMapToSave.isEmpty()) {
            defaultPropertyRepository.saveAll(propertyMapToSave.values());
            log.info("패키지 {} 내 아티팩트들의 Default Value {} 건이 DB에 저장되었습니다.", packageId, propertyMapToSave.size());
        }
    }
}
