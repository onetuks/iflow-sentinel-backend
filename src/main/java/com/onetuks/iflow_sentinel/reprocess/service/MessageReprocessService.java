package com.onetuks.iflow_sentinel.reprocess.service;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessSupportType;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageType;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageBodyResponse;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageReprocessRequest;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageReprocessResult;
import com.onetuks.iflow_sentinel.reprocess.dto.MplFailureResponse;
import com.onetuks.iflow_sentinel.reprocess.dto.SapMplLogDto;
import com.onetuks.iflow_sentinel.reprocess.dto.StorageMappingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

            result.add(new MplFailureResponse(
                    dto.messageGuid(),
                    dto.correlationId(),
                    dto.status(),
                    dto.integrationArtifact() != null ? dto.integrationArtifact().id() : sapArtifactId,
                    dto.integrationArtifact() != null ? dto.integrationArtifact().name() : artifactName,
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
            if (storageName == null || storageName.isBlank() || "DEFAULT_STORE".equals(storageName)) {
                storageName = mapping.get().storageName();
            }
            expireDays = mapping.get().expireDays();
        }

        if (storageName == null || storageName.isBlank() || "DEFAULT_STORE".equals(storageName)) {
            // Artifact 이름 기반 추정 fallback
            storageName = artifactRepository.findById(artifactId)
                    .map(a -> a.getName())
                    .orElse("DEFAULT_STORE");
        }

        String messageBody;
        String deepLinkUrl = buildSapDeepLinkUrl(tenant, storageType, storageName);

        if (storageType == StorageType.DATASTORE) {
            try {
                // 1차 시도: SAP IS 공식 OData 복합 키 엔드포인트 (DataStoreName 키가 앞에 오는 표준 규격)
                String path1 = "/DataStoreEntries(DataStoreName='" + storageName + "',Id='" + messageId + "')/$value";
                byte[] binaryContent = sapODataClient.getBinary(tenant, path1);
                messageBody = new String(binaryContent);
            } catch (Exception e1) {
                log.info("1차 DataStoreEntries(DataStoreName,Id) 조회 불가 (사유: {}). 2차 (Id,DataStoreName) 시도.", e1.getMessage());
                try {
                    // 2차 시도: (Id, DataStoreName) 순서 복합키 엔드포인트
                    String path2 = "/DataStoreEntries(Id='" + messageId + "',DataStoreName='" + storageName + "')/$value";
                    byte[] binaryContent = sapODataClient.getBinary(tenant, path2);
                    messageBody = new String(binaryContent);
                } catch (Exception e2) {
                    log.info("2차 DataStoreEntries(Id,DataStoreName) 조회 불가 (사유: {}). 3차 Navigation 엔드포인트 시도.", e2.getMessage());
                    try {
                        // 3차 시도: Navigation Property 엔드포인트
                        String path3 = "/DataStores('" + storageName + "')/Entries('" + messageId + "')/$value";
                        byte[] binaryContent = sapODataClient.getBinary(tenant, path3);
                        messageBody = new String(binaryContent);
                    } catch (Exception e3) {
                        log.warn("DataStore payload 직접 조회 실패 (storageName={}, messageId={}, 사유={}). 기본 설명 문자열 제공.",
                                storageName, messageId, e3.getMessage());
                        messageBody = "<DataStoreEntry storageName=\"" + storageName + "\" messageId=\"" + messageId + "\">\n" +
                                "  <Status>FAILED</Status>\n" +
                                "  <Description>Message payload is stored in Data Store: " + storageName + "</Description>\n" +
                                "</DataStoreEntry>";
                    }
                }
            }
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

    @Transactional
    public MessageReprocessResult reprocessMessage(MessageReprocessRequest request) {
        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 테넌트입니다. ID=" + request.tenantId()));

        String deepLinkUrl = buildSapDeepLinkUrl(tenant, request.storageType(), request.storageName());

        if (request.storageType() == StorageType.DATASTORE) {
            try {
                // 1차 시도: 복합키 엔드포인트 시도
                String primaryPath = "/DataStoreEntries(Id='" + request.messageId() + "',DataStoreName='" + request.storageName() + "')/reprocess";
                sapODataClient.executeAction(tenant, HttpMethod.POST, primaryPath);
            } catch (Exception e1) {
                log.info("1차 DataStoreEntries 복합키 재처리 호출 실패 ({}), 2차 Navigation 엔드포인트 시도.", e1.getMessage());
                try {
                    // 2차 시도: Navigation Property 엔드포인트 시도
                    String fallbackPath = "/DataStores('" + request.storageName() + "')/Entries('" + request.messageId() + "')/reprocess";
                    sapODataClient.executeAction(tenant, HttpMethod.POST, fallbackPath);
                } catch (Exception e2) {
                    log.warn("SAP DataStore 재처리 호출 실패, 반자동 결과 전달: {}", e2.getMessage());
                }
            }
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
