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
                } catch (NumberFormatException ignored) {
                }
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

        // 2) Status (FAILED, ESCALATED, CANCELLED), SubStatus, CustomStatus (소문자
        // 'fail', 'err', 'cancel' 포함) 에러 건 검증
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
                .orElseGet(() -> jmsMapping.filter(m -> m != null && m.storageName() != null).map(m -> m.storageName())
                        .orElse("N/A"));
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
                // $expand가 거부되는 SAP OData 501 회피를 위해 개별 평문 에러 조회 호출
                // (/MessageProcessingLogErrorInformations('{messageGuid}')/$value)
                errorDetail = sapODataClient.getMplLogErrorInformation(tenant, dto.messageGuid());
            }
            if (errorDetail == null || errorDetail.isBlank()) {
                errorDetail = "Status: " + dto.status() + " (No detailed error text returned by SAP IS)";
            }

            String effectiveArtifactId = dto.getArtifactIdOrName();
            if (effectiveArtifactId == null || effectiveArtifactId.isBlank()) {
                effectiveArtifactId = sapArtifactId;
            }
            String effectiveArtifactName = (dto.integrationArtifact() != null
                    && dto.integrationArtifact().name() != null && !dto.integrationArtifact().name().isBlank())
                            ? dto.integrationArtifact().name()
                            : (dto.integrationFlowName() != null && !dto.integrationFlowName().isBlank()
                                    ? dto.integrationFlowName()
                                    : artifactName);

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
                    errorDetail));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public MessageBodyResponse getMessageBody(Long tenantId, Long artifactId, String messageId,
            StorageType storageType) {
        return getMessageBody(tenantId, artifactId, messageId, storageType, null);
    }

    @Transactional(readOnly = true)
    public MessageBodyResponse getMessageBody(Long tenantId, Long artifactId, String messageId, StorageType storageType,
            String requestedStorageName) {
        log.info("메시지 바디 조회 요청 시작 - tenantId: {}, artifactId: {}, messageId: {}, storageType: {}, requestedStorageName: {}",
                tenantId, artifactId, messageId, storageType, requestedStorageName);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 테넌트입니다. ID=" + tenantId));

        String storageName = requestedStorageName;
        Integer expireDays = 30;

        Optional<StorageMappingDto> mapping = storageMappingService.getStorageMapping(tenantId, artifactId,
                storageType);
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

        try {
            if (storageType == StorageType.DATASTORE) {
                messageBody = fetchBinaryPayload(tenant, messageId);
            } else { // JMS
                messageBody = "<!-- JMS Queue Payload is managed by SAP Integration Suite JMS Engine. -->\n" +
                        "<!-- Queue: " + storageName + " | MessageId: " + messageId + " -->\n" +
                        "<!-- 개별 JMS 메시지 바디는 Web UI (Manage Queues)를 통해 확인하거나 모니터링 링크를 이용해주세요. -->";
            }
            log.info("메시지 바디 조회 성공 - messageId: {}, storageType: {}, storageName: {}, bodyLength: {}",
                    messageId, storageType, storageName, messageBody != null ? messageBody.length() : 0);
        } catch (Exception e) {
            log.error("메시지 바디 조회 실패 - messageId: {}, storageType: {}, storageName: {}, 사유: {}",
                    messageId, storageType, storageName, e.getMessage(), e);
            throw e;
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
                deepLinkUrl);
    }

    /**
     * MessageId로 DataStoreEntries에서 엔트리를 찾는다. SAP OData의 Id는 MessageId가 아니라
     * "{IntegrationFlow}_{Timestamp}_{Sender|Receiver}_{MessageId}" 형태의 복합 문자열이므로,
     * 정확한 복합키(Id, DataStoreName, IntegrationFlow, Type)를 얻으려면 실제 엔트리를 조회해
     * 확인해야 한다. DataStoreEntries는 MessageId에 대한 $filter를 지원하지 않아(400 Bad
     * Request) 페이지(__next)를 순회하며 매칭되는 항목을 찾는다.
     */
    private SapDataStoreEntryDto findDataStoreEntry(Tenant tenant, String messageId) {
        log.info("DataStore 엔트리 검색 API 호출 - tenantId: {}, messageId: {}", tenant.getId(), messageId);
        try {
            SapDataStoreEntryDto entry = sapODataClient.findInCollection(tenant, "/DataStoreEntries",
                    new ParameterizedTypeReference<ODataCollectionResponse<SapDataStoreEntryDto>>() {
                    },
                    e -> messageId.equalsIgnoreCase(e.messageId()))
                    .orElseThrow(() -> {
                        log.warn("DataStore 엔트리 검색 실패 - MessageId={} 에 해당하는 엔트리가 없습니다.", messageId);
                        return new ConnectorException(
                                "DataStore에서 MessageId=" + messageId + "에 해당하는 엔트리를 찾을 수 없습니다.", 404);
                    });
            log.info("DataStore 엔트리 검색 성공 - messageId: {}, found entry Id: {}, DataStoreName: {}, IntegrationFlow: {}",
                    messageId, entry.id(), entry.dataStoreName(), entry.integrationFlow());
            return entry;
        } catch (Exception e) {
            log.error("DataStore 엔트리 검색 API 예외 발생 - messageId: {}, 사유: {}", messageId, e.getMessage(), e);
            throw e;
        }
    }

    private String buildEntryKeyPath(SapDataStoreEntryDto entry) {
        return "/DataStoreEntries(Id='" + encodeODataKey(entry.id()) +
                "',DataStoreName='" + encodeODataKey(entry.dataStoreName()) +
                "',IntegrationFlow='" + encodeODataKey(entry.integrationFlow()) +
                "',Type='" + encodeODataKey(entry.type()) + "')";
    }

    private String fetchBinaryPayload(Tenant tenant, String messageId) {
        log.info("DataStore 바이너리 Payload 페치 과정 시작 - messageId: {}", messageId);
        SapDataStoreEntryDto entry = findDataStoreEntry(tenant, messageId);
        String keyPath = buildEntryKeyPath(entry) + "/$value";
        log.info("DataStore 바이너리 Payload API 호출 - keyPath: {}", keyPath);
        byte[] binaryContent = sapODataClient.getBinary(tenant, keyPath);
        String extracted = extractPayloadFromBinary(binaryContent);
        log.info("DataStore 바이너리 Payload 추출 완료 - messageId: {}, length: {}", messageId, extracted.length());
        return extracted;
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
            log.warn("추출할 바이너리 Content가 비어있습니다 (0 bytes).");
            return "";
        }
        log.info("바이너리 Content 변환/압축해제 시도 - rawBinaryBytes: {} bytes", binaryContent.length);
        // ZIP 파일 매직 넘버 확인 (PK\003\004)
        if (binaryContent.length >= 4 &&
                binaryContent[0] == 0x50 && binaryContent[1] == 0x4B &&
                binaryContent[2] == 0x03 && binaryContent[3] == 0x04) {
            log.info("바이너리 Content가 ZIP 포맷입니다. ZipInputStream으로 압축 해제를 진행합니다.");
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
                    log.debug("Zip 내부 파일 확인 - name: {}, size: {} bytes", zipEntry.getName(), content.length);
                    if (name.contains("body") || name.contains("payload") || name.contains("message")) {
                        bodyBytes = content;
                        log.info("Zip 본문 타겟 엔트리 감지: {}", zipEntry.getName());
                        break;
                    }
                    if (firstFileBytes == null) {
                        firstFileBytes = content;
                    }
                }
                byte[] targetBytes = bodyBytes != null ? bodyBytes : firstFileBytes;
                if (targetBytes != null) {
                    log.info("Zip 텍스트 추출 완료 - extractedSize: {} bytes", targetBytes.length);
                    return new String(targetBytes, java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                log.warn("Zip 파일 압축 해제 실패, 원본 바이너리를 텍스트로 직접 변환합니다: {}", e.getMessage(), e);
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
            executeDataStoreReprocess(tenant, request.messageId());
            return new MessageReprocessResult(
                    request.messageId(),
                    true,
                    "Data Store (" + request.storageName() + ") 메시지 재처리 요청을 전달했습니다.",
                    request.storageType().name(),
                    request.storageName(),
                    LocalDateTime.now(),
                    deepLinkUrl);
        } else {
            // JMS 큐 메시지는 Web UI 매핑 안내 반환
            return new MessageReprocessResult(
                    request.messageId(),
                    true,
                    "JMS 큐 (" + request.storageName() + ") 메시지 재처리를 위해 SAP IS Manage Queues 바로가기 링크가 생성되었습니다.",
                    request.storageType().name(),
                    request.storageName(),
                    LocalDateTime.now(),
                    deepLinkUrl);
        }
    }

    private void executeDataStoreReprocess(Tenant tenant, String messageId) {
        SapDataStoreEntryDto entry = findDataStoreEntry(tenant, messageId);
        sapODataClient.executeAction(tenant, HttpMethod.POST, buildEntryKeyPath(entry) + "/reprocess");
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
        String baseUrl = tenant.getOdataUrl() != null ? tenant.getOdataUrl()
                : "https://sap-integration-suite.cfapps.sap";
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
            if (art.equalsIgnoreCase(targetArtifactId) || art.contains(targetArtifactId)
                    || targetArtifactId.contains(art)) {
                return true;
            }
        }
        return false;
    }

    private boolean isErrorStatus(String status, String subStatus, String customStatus) {
        if (status != null) {
            String stUpper = status.toUpperCase();
            if (stUpper.contains("FAIL") || stUpper.contains("ESCALAT") || stUpper.contains("CANCEL")
                    || stUpper.contains("ERR")) {
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
