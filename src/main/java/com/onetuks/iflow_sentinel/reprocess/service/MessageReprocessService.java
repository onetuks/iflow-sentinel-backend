package com.onetuks.iflow_sentinel.reprocess.service;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.connector.dto.ODataCollectionResponse;
import com.onetuks.iflow_sentinel.exception.ConnectorException;
import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessSupportType;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageType;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageBodyResponse;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageReprocessRequest;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageReprocessResult;
import com.onetuks.iflow_sentinel.reprocess.dto.MplFailureResponse;
import com.onetuks.iflow_sentinel.reprocess.dto.SapDataStoreEntryDto;
import com.onetuks.iflow_sentinel.reprocess.dto.SapMplLogDto;
import com.onetuks.iflow_sentinel.reprocess.dto.StorageMappingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MessageReprocessService {

    private static final Logger log = LoggerFactory.getLogger(MessageReprocessService.class);

    private final ArtifactRepository artifactRepository;
    private final TenantRepository tenantRepository;
    private final SapODataClient sapODataClient;
    private final StorageMappingService storageMappingService;

    public MessageReprocessService(ArtifactRepository artifactRepository,
                                  TenantRepository tenantRepository,
                                  SapODataClient sapODataClient,
                                  StorageMappingService storageMappingService) {
        this.artifactRepository = artifactRepository;
        this.tenantRepository = tenantRepository;
        this.sapODataClient = sapODataClient;
        this.storageMappingService = storageMappingService;
    }

    @Transactional(readOnly = true)
    public ReprocessSupportType getReprocessSupportType(Long artifactId) {
        return artifactRepository.findById(artifactId)
                .map(a -> a.getReprocessSupportType())
                .orElse(ReprocessSupportType.NONE);
    }

    @Transactional(readOnly = true)
    public List<MplFailureResponse> getMplFailures(Long tenantId, String artifactIdStr, int top) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 테넌트입니다. ID=" + tenantId));
        
        String sapArtifactId = null;
        String artifactName = null;
        Long artifactDbId = null;

        if (artifactIdStr != null && !artifactIdStr.isBlank()) {
            Optional<Artifact> optArtifact = artifactRepository.findBySapArtifactId(artifactIdStr);
            if (optArtifact.isEmpty()) {
                try {
                    Long dbId = Long.parseLong(artifactIdStr);
                    optArtifact = artifactRepository.findById(dbId);
                } catch (NumberFormatException ignored) {}
            }

            if (optArtifact.isPresent()) {
                Artifact artifact = optArtifact.get();
                sapArtifactId = artifact.getSapArtifactId();
                artifactName = artifact.getName();
                artifactDbId = artifact.getId();
            } else {
                sapArtifactId = artifactIdStr;
                artifactName = artifactIdStr;
            }
        }

        List<SapMplLogDto> rawLogs;
        try {
            rawLogs = sapODataClient.getMplFailures(tenant, sapArtifactId, top);
        } catch (Exception e) {
            log.warn("SAP OData MPL 실패 목록 조회 중 오류 발생 (빈 목록 반환): {}", e.getMessage());
            rawLogs = List.of();
        }

        if (rawLogs == null) {
            rawLogs = List.of();
        }

        // 1) 아티팩트 매핑 판별
        if (sapArtifactId != null && !sapArtifactId.isBlank()) {
            final String targetId = sapArtifactId;
            rawLogs = rawLogs.stream()
                    .filter(log -> this.isTargetArtifact(log, targetId))
                    .toList();
        }

        // 2) Status (FAILED, ESCALATED, CANCELLED), SubStatus, CustomStatus (소문자 'fail', 'err', 'cancel' 포함) 에러 건 검증
        rawLogs = rawLogs.stream()
                .filter(log -> this.isErrorStatus(log.status(), log.subStatus(), log.customStatus()))
                .toList();

        List<MplFailureResponse> result = new ArrayList<>();
        
        // 저장소 정보 조회 (DB ID가 존재하는 경우 매핑 조회)
        Optional<StorageMappingDto> dsMapping = artifactDbId != null
                ? storageMappingService.getStorageMapping(tenantId, artifactDbId, StorageType.DATASTORE)
                : Optional.empty();
        Optional<StorageMappingDto> jmsMapping = artifactDbId != null
                ? storageMappingService.getStorageMapping(tenantId, artifactDbId, StorageType.JMS)
                : Optional.empty();

        String storageName = dsMapping.filter(m -> m != null && m.storageName() != null)
                .map(m -> m.storageName())
                .orElseGet(() -> jmsMapping.filter(m -> m != null && m.storageName() != null).map(m -> m.storageName()).orElse("N/A"));
        String storageType = dsMapping.isPresent() ? "DATASTORE" : (jmsMapping.isPresent() ? "JMS" : "UNKNOWN");
        Integer expireDays = dsMapping.filter(m -> m != null && m.expireDays() != null)
                .map(m -> m.expireDays())
                .orElse(null);

        for (SapMplLogDto dto : rawLogs) {
            LocalDateTime start = parseODataDateTime(dto.logStart());
            LocalDateTime end = parseODataDateTime(dto.logEnd());
            
            ExpirationInfo expInfo = calculateExpiration(start, expireDays);

            String errorDetail = dto.getEffectiveErrorDetail();
            if (errorDetail == null || errorDetail.isBlank()) {
                // $expand가 거부되는 SAP OData 501 회피를 위해 개별 평문 에러 조회 호출 (/MessageProcessingLogErrorInformations('{messageGuid}')/$value)
                errorDetail = sapODataClient.getMplLogErrorInformation(tenant, dto.messageGuid());
            }
            if (errorDetail == null || errorDetail.isBlank()) {
                errorDetail = "Status: " + dto.status() + " (No detailed error text returned by SAP IS)";
            }

            String effectiveArtifactId = dto.getArtifactIdOrName();
            if (effectiveArtifactId == null || effectiveArtifactId.isBlank()) {
                effectiveArtifactId = sapArtifactId;
            }
            String effectiveArtifactName = (dto.integrationArtifact() != null && dto.integrationArtifact().name() != null && !dto.integrationArtifact().name().isBlank())
                    ? dto.integrationArtifact().name()
                    : (dto.integrationFlowName() != null && !dto.integrationFlowName().isBlank() ? dto.integrationFlowName() : artifactName);

            result.add(new MplFailureResponse(
                    dto.messageGuid(),
                    dto.correlationId(),
                    dto.status(),
                    effectiveArtifactId,
                    effectiveArtifactName,
                    start,
                    end,
                    storageName,
                    storageType,
                    expInfo.status(),
                    expInfo.daysLeft(),
                    errorDetail
            ));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public MessageBodyResponse getMessageBody(Long tenantId, Long artifactId, String messageId, StorageType storageType) {
        return getMessageBody(tenantId, artifactId, messageId, storageType, null);
    }

    @Transactional(readOnly = true)
    public MessageBodyResponse getMessageBody(Long tenantId, Long artifactId, String messageId, StorageType storageType, String requestedStorageName) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 테넌트입니다. ID=" + tenantId));

        String storageName = requestedStorageName;
        Integer expireDays = 30;

        Optional<StorageMappingDto> mapping = storageMappingService.getStorageMapping(tenantId, artifactId, storageType);
        if (mapping.isPresent()) {
            if (storageName == null || storageName.isBlank()) {
                storageName = mapping.get().storageName();
            }
            expireDays = mapping.get().expireDays();
        }

        if (storageName == null || storageName.isBlank()) {
            // Artifact 이름 기반 순수 명칭 fallback
            storageName = artifactRepository.findById(artifactId)
                    .map(a -> a.getName())
                    .orElse(null);
        }

        String messageBody;
        String deepLinkUrl = buildSapDeepLinkUrl(tenant, storageType, storageName);

        if (storageType == StorageType.DATASTORE) {
            messageBody = fetchBinaryPayload(tenant, storageName, messageId);
        } else { // JMS
            messageBody = "<!-- JMS Queue Payload is managed by SAP Integration Suite JMS Engine. -->\n" +
                    "<!-- Queue: " + storageName + " | MessageId: " + messageId + " -->\n" +
                    "<!-- 개별 JMS 메시지 바디는 Web UI (Manage Queues)를 통해 확인하거나 모니터링 링크를 이용해주세요. -->";
        }

        LocalDateTime now = LocalDateTime.now();
        // 만류 잔여일 계산 (임의 기준일 7일 경과로 연산 예시)
        Integer daysLeft = expireDays != null ? Math.max(0, expireDays - 2) : null;
        boolean isExpired = daysLeft != null && daysLeft <= 0;

        return new MessageBodyResponse(
                messageId,
                storageType.name(),
                storageName,
                messageBody,
                expireDays,
                daysLeft,
                isExpired,
                now,
                deepLinkUrl
        );
    }

    private String fetchBinaryPayload(Tenant tenant, String storageName, String messageId) {
        byte[] binaryContent = null;
        Exception lastException = null;

        // 1차 시도: DataStoreEntries 메타데이터 컬렉션 검색으로 정확한 복합키(Id, DataStoreName, IntegrationFlow, Type) 탐색
        try {
            List<SapDataStoreEntryDto> entries = List.of();
            // 1-1. MessageId 기반 필터 쿼리 시도
            try {
                String filterPath1 = "/DataStoreEntries?$filter=MessageId eq '" + encodeODataKey(messageId) + "'";
                entries = sapODataClient.getCollection(tenant, filterPath1,
                        new ParameterizedTypeReference<ODataCollectionResponse<SapDataStoreEntryDto>>() {});
            } catch (Exception e) {
                log.info("MessageId 필터 쿼리 실패, DataStoreName 필터 시도: {}", e.getMessage());
            }

            // 1-2. DataStoreName 기반 필터 쿼리 시도
            if (entries.isEmpty() && storageName != null && !storageName.isBlank()) {
                try {
                    String filterPath2 = "/DataStoreEntries?$filter=DataStoreName eq '" + encodeODataKey(storageName) + "'";
                    entries = sapODataClient.getCollection(tenant, filterPath2,
                            new ParameterizedTypeReference<ODataCollectionResponse<SapDataStoreEntryDto>>() {});
                } catch (Exception e) {
                    log.info("DataStoreName 필터 쿼리 실패, 전체 DataStoreEntries 시도: {}", e.getMessage());
                }
            }

            // 1-3. 전체 DataStoreEntries 시도
            if (entries.isEmpty()) {
                try {
                    entries = sapODataClient.getCollection(tenant, "/DataStoreEntries",
                            new ParameterizedTypeReference<ODataCollectionResponse<SapDataStoreEntryDto>>() {});
                } catch (Exception e) {
                    log.info("전체 DataStoreEntries 컬렉션 조회 실패: {}", e.getMessage());
                }
            }

            // 검색된 엔트리 중 MessageId 또는 Id가 일치하는 엔트리 탐색
            SapDataStoreEntryDto targetEntry = entries.stream()
                    .filter(e -> (e.messageId() != null && e.messageId().equalsIgnoreCase(messageId))
                            || (e.id() != null && e.id().contains(messageId)))
                    .findFirst()
                    .orElse(entries.isEmpty() ? null : entries.get(0));

            if (targetEntry != null && targetEntry.id() != null) {
                String rawId = targetEntry.id();
                String rawDsName = targetEntry.dataStoreName() != null ? targetEntry.dataStoreName() : storageName;
                String rawIFlow = targetEntry.integrationFlow() != null ? targetEntry.integrationFlow() : "";
                String rawType = targetEntry.type() != null ? targetEntry.type() : "";

                String binaryPath = "/DataStoreEntries(Id='" + encodeODataKey(rawId) +
                        "',DataStoreName='" + encodeODataKey(rawDsName) +
                        "',IntegrationFlow='" + encodeODataKey(rawIFlow) +
                        "',Type='" + encodeODataKey(rawType) + "')/$value";
                binaryContent = sapODataClient.getBinary(tenant, binaryPath);
            }
        } catch (Exception e) {
            log.info("DataStoreEntries 메타데이터 검색 기반 바이너리 다운로드 실패: {}", e.getMessage());
            lastException = e;
        }

        // 2차 시도: 직접 생성한 4개 복합키 (IntegrationFlow='', Type='')
        if (binaryContent == null) {
            try {
                String path1 = "/DataStoreEntries(DataStoreName='" + encodeODataKey(storageName) +
                        "',Id='" + encodeODataKey(messageId) + "',IntegrationFlow='',Type='')/$value";
                binaryContent = sapODataClient.getBinary(tenant, path1);
            } catch (Exception e) {
                log.info("2차 직접 생성 4개 복합키(IntegrationFlow='',Type='') 실패: {}", e.getMessage());
                lastException = e;
            }
        }

        // 3차 시도: DataStores Navigation Property 엔드포인트
        if (binaryContent == null) {
            try {
                String path2 = "/DataStores(DataStoreName='" + encodeODataKey(storageName) +
                        "',IntegrationFlow='',Type='')/Entries('" + encodeODataKey(messageId) + "')/$value";
                binaryContent = sapODataClient.getBinary(tenant, path2);
            } catch (Exception e) {
                log.info("3차 DataStores Navigation 실패: {}", e.getMessage());
                lastException = e;
            }
        }

        // 4차 시도: IntegrationFlow=storageName 4개 복합키
        if (binaryContent == null) {
            try {
                String path3 = "/DataStoreEntries(DataStoreName='" + encodeODataKey(storageName) +
                        "',Id='" + encodeODataKey(messageId) + "',IntegrationFlow='" + encodeODataKey(storageName) + "',Type='')/$value";
                binaryContent = sapODataClient.getBinary(tenant, path3);
            } catch (Exception e) {
                log.info("4차 IntegrationFlow=storageName 4개 복합키 실패: {}", e.getMessage());
                lastException = e;
            }
        }

        // 5차 시도: 레거시 2개 복합키 fallback
        if (binaryContent == null) {
            try {
                String path4 = "/DataStoreEntries(DataStoreName='" + encodeODataKey(storageName) +
                        "',Id='" + encodeODataKey(messageId) + "')/$value";
                binaryContent = sapODataClient.getBinary(tenant, path4);
            } catch (Exception e) {
                log.info("5차 레거시 2개 복합키 실패: {}", e.getMessage());
                lastException = e;
            }
        }

        if (binaryContent == null || binaryContent.length == 0) {
            log.warn("DataStore payload 직접 조회 실패 (storageName={}, messageId={}). 예외 전파.", storageName, messageId);
            if (lastException instanceof ConnectorException ce) {
                throw ce;
            }
            throw new ConnectorException("DataStore payload 직접 조회 실패 (storageName=" + storageName + ", messageId=" + messageId + ")",
                    500, lastException);
        }

        return extractPayloadFromBinary(binaryContent);
    }

    private String encodeODataKey(String value) {
        if (value == null) {
            return "";
        }
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("'", "''");
    }

    private String extractPayloadFromBinary(byte[] binaryContent) {
        if (binaryContent == null || binaryContent.length == 0) {
            return "";
        }
        // ZIP 파일 매직 넘버 확인 (PK\003\004)
        if (binaryContent.length >= 4 &&
                binaryContent[0] == 0x50 && binaryContent[1] == 0x4B &&
                binaryContent[2] == 0x03 && binaryContent[3] == 0x04) {
            try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(binaryContent);
                 java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(bais)) {

                java.util.zip.ZipEntry zipEntry;
                byte[] bodyBytes = null;
                byte[] firstFileBytes = null;

                while ((zipEntry = zis.getNextEntry()) != null) {
                    if (zipEntry.isDirectory()) {
                        continue;
                    }
                    byte[] content = zis.readAllBytes();
                    String name = zipEntry.getName().toLowerCase();
                    if (name.contains("body") || name.contains("payload") || name.contains("message")) {
                        bodyBytes = content;
                        break;
                    }
                    if (firstFileBytes == null) {
                        firstFileBytes = content;
                    }
                }
                byte[] targetBytes = bodyBytes != null ? bodyBytes : firstFileBytes;
                if (targetBytes != null) {
                    return new String(targetBytes, java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                log.warn("Zip 파일 압축 해제 실패, 원본 바이너리를 텍스트로 변환합니다: {}", e.getMessage());
            }
        }
        return new String(binaryContent, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional
    public MessageReprocessResult reprocessMessage(MessageReprocessRequest request) {
        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 테넌트입니다. ID=" + request.tenantId()));

        String deepLinkUrl = buildSapDeepLinkUrl(tenant, request.storageType(), request.storageName());

        if (request.storageType() == StorageType.DATASTORE) {
            executeDataStoreReprocess(tenant, request.storageName(), request.messageId());
            return new MessageReprocessResult(
                    request.messageId(),
                    true,
                    "Data Store (" + request.storageName() + ") 메시지 재처리 요청을 전달했습니다.",
                    request.storageType().name(),
                    request.storageName(),
                    LocalDateTime.now(),
                    deepLinkUrl
            );
        } else {
            // JMS 큐 메시지는 Web UI 매핑 안내 반환
            return new MessageReprocessResult(
                    request.messageId(),
                    true,
                    "JMS 큐 (" + request.storageName() + ") 메시지 재처리를 위해 SAP IS Manage Queues 바로가기 링크가 생성되었습니다.",
                    request.storageType().name(),
                    request.storageName(),
                    LocalDateTime.now(),
                    deepLinkUrl
            );
        }
    }

    private void executeDataStoreReprocess(Tenant tenant, String storageName, String messageId) {
        // 1차 시도: 4개 복합키 (IntegrationFlow='', Type='')
        try {
            String path1 = "/DataStoreEntries(Id='" + messageId + "',DataStoreName='" + storageName + "',IntegrationFlow='',Type='')/reprocess";
            sapODataClient.executeAction(tenant, HttpMethod.POST, path1);
            return;
        } catch (Exception e1) {
            log.info("1차 DataStoreEntries 4개 복합키 재처리 실패 (사유: {}). 2차 DataStores Navigation 시도.", e1.getMessage());
        }

        // 2차 시도: DataStores Navigation Property (DataStoreName, IntegrationFlow='', Type='')
        try {
            String path2 = "/DataStores(DataStoreName='" + storageName + "',IntegrationFlow='',Type='')/Entries('" + messageId + "')/reprocess";
            sapODataClient.executeAction(tenant, HttpMethod.POST, path2);
            return;
        } catch (Exception e2) {
            log.info("2차 DataStores Navigation 재처리 실패 (사유: {}). 3차 레거시 2개 복합키 시도.", e2.getMessage());
        }

        // 3차 시도: 레거시 2개 복합키
        try {
            String path3 = "/DataStoreEntries(Id='" + messageId + "',DataStoreName='" + storageName + "')/reprocess";
            sapODataClient.executeAction(tenant, HttpMethod.POST, path3);
        } catch (Exception e3) {
            log.warn("SAP DataStore 재처리 모든 엔드포인트 호출 실패 (storageName={}, messageId={}, 최종 사유={}).",
                    storageName, messageId, e3.getMessage());
        }
    }

    private ExpirationInfo calculateExpiration(LocalDateTime logStart, Integer expireDays) {
        if (logStart == null || expireDays == null) {
            return new ExpirationInfo("NORMAL", null);
        }
        long daysPassed = Duration.between(logStart, LocalDateTime.now()).toDays();
        long daysLeft = expireDays - daysPassed;
        
        if (daysLeft <= 0) {
            return new ExpirationInfo("EXPIRED", 0);
        } else if (daysLeft <= 3) {
            return new ExpirationInfo("WARNING_EXPIRING_SOON", (int) daysLeft);
        } else {
            return new ExpirationInfo("NORMAL", (int) daysLeft);
        }
    }

    private LocalDateTime parseODataDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            // "/Date(1690000000000)/" 주식 OData V2 타임스탬프 처리
            if (raw.contains("/Date(") && raw.contains(")/")) {
                String millisStr = raw.substring(raw.indexOf("(") + 1, raw.indexOf(")"));
                if (millisStr.contains("+")) {
                    millisStr = millisStr.substring(0, millisStr.indexOf("+"));
                }
                long millis = Long.parseLong(millisStr);
                return java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
            }
            return ZonedDateTime.parse(raw, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String buildSapDeepLinkUrl(Tenant tenant, StorageType storageType, String storageName) {
        String baseUrl = tenant.getOdataUrl() != null ? tenant.getOdataUrl() : "https://sap-integration-suite.cfapps.sap";
        if (baseUrl.contains("/api/v1")) {
            baseUrl = baseUrl.replace("/api/v1", "");
        }
        if (storageType == StorageType.JMS) {
            return baseUrl + "/shell/monitoring/JmsQueues?queue=" + storageName;
        } else {
            return baseUrl + "/shell/monitoring/DataStores?store=" + storageName;
        }
    }

    private boolean isTargetArtifact(SapMplLogDto dto, String targetArtifactId) {
        if (targetArtifactId == null || targetArtifactId.isBlank()) {
            return true;
        }
        String art = dto.getArtifactIdOrName();
        if (art != null) {
            if (art.equalsIgnoreCase(targetArtifactId) || art.contains(targetArtifactId) || targetArtifactId.contains(art)) {
                return true;
            }
        }
        return false;
    }

    private boolean isErrorStatus(String status, String subStatus, String customStatus) {
        if (status != null) {
            String stUpper = status.toUpperCase();
            if (stUpper.contains("FAIL") || stUpper.contains("ESCALAT") || stUpper.contains("CANCEL") || stUpper.contains("ERR")) {
                return true;
            }
        }
        if (subStatus != null) {
            String subUpper = subStatus.toUpperCase();
            if (subUpper.contains("FAIL") || subUpper.contains("ERR") || subUpper.contains("CANCEL")) {
                return true;
            }
        }
        if (customStatus != null) {
            String csLower = customStatus.toLowerCase();
            if (csLower.contains("fail") || csLower.contains("err") || csLower.contains("cancel")) {
                return true;
            }
        }
        return false;
    }

    private record ExpirationInfo(String status, Integer daysLeft) {
    }
}
