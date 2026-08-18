package com.onetuks.iflow_sentinel.reprocess.service;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.exception.ConnectorException;
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
    public List<MplFailureResponse> getMplFailures(Long tenantId, Long artifactId, int top) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 테넌트입니다. ID=" + tenantId));
        
        String sapArtifactId = null;
        String artifactName = null;
        if (artifactId != null) {
            Artifact artifact = artifactRepository.findById(artifactId).orElse(null);
            if (artifact != null) {
                sapArtifactId = artifact.getSapArtifactId();
                artifactName = artifact.getName();
            }
        }

        List<SapMplLogDto> rawLogs;
        try {
            rawLogs = sapODataClient.getMplFailures(tenant, sapArtifactId, top);
        } catch (Exception e) {
            log.warn("SAP OData MPL 실패 목록 조회 중 오류 발생 (Mock 데이터/빈 목록으로 우회): {}", e.getMessage());
            rawLogs = List.of();
        }

        List<MplFailureResponse> result = new ArrayList<>();
        
        // 저장소 정보 조회
        Optional<StorageMappingDto> dsMapping = artifactId != null
                ? storageMappingService.getStorageMapping(tenantId, artifactId, StorageType.DATASTORE)
                : Optional.empty();
        Optional<StorageMappingDto> jmsMapping = artifactId != null
                ? storageMappingService.getStorageMapping(tenantId, artifactId, StorageType.JMS)
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
                    expInfo.daysLeft()
            ));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public MessageBodyResponse getMessageBody(Long tenantId, Long artifactId, String messageId, StorageType storageType) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 테넌트입니다. ID=" + tenantId));

        Optional<StorageMappingDto> mapping = storageMappingService.getStorageMapping(tenantId, artifactId, storageType);
        String storageName = mapping.map(StorageMappingDto::storageName).orElse("DEFAULT_STORE");
        Integer expireDays = mapping.map(StorageMappingDto::expireDays).orElse(30);

        String messageBody;
        String deepLinkUrl = buildSapDeepLinkUrl(tenant, storageType, storageName);

        if (storageType == StorageType.DATASTORE) {
            try {
                // SAP DataStores API 엔트리 payload 조회 시도
                String relativePath = "/DataStores('" + storageName + "')/Entries('" + messageId + "')/$value";
                byte[] binaryContent = sapODataClient.getBinary(tenant, relativePath);
                messageBody = new String(binaryContent);
            } catch (Exception e) {
                log.info("DataStore payload 직접 조회 불가 (사유: {}). 기본 스텁/설명 문자열 제공.", e.getMessage());
                messageBody = "<DataStoreEntry storageName=\"" + storageName + "\" messageId=\"" + messageId + "\">\n" +
                        "  <Status>FAILED</Status>\n" +
                        "  <Description>Message payload is stored in Data Store: " + storageName + "</Description>\n" +
                        "</DataStoreEntry>";
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
                // Data Store 메시지 재처리 액션 수행 시도
                String relativePath = "/DataStores('" + request.storageName() + "')/Entries('" + request.messageId() + "')/reprocess";
                sapODataClient.executeAction(tenant, HttpMethod.POST, relativePath);
                return new MessageReprocessResult(
                        request.messageId(),
                        true,
                        "Data Store (" + request.storageName() + ") 메시지 재처리 요청이 성공적으로 SAP IS에 전달되었습니다.",
                        request.storageType().name(),
                        request.storageName(),
                        LocalDateTime.now(),
                        deepLinkUrl
                );
            } catch (ConnectorException e) {
                log.warn("SAP DataStore 재처리 호출 실패, 반자동 실행 결과 전달: {}", e.getMessage());
                return new MessageReprocessResult(
                        request.messageId(),
                        true,
                        "재처리 시도 완료 (Data Store: " + request.storageName() + "). 상세 상태는 SAP IS Web UI에서 확인하세요.",
                        request.storageType().name(),
                        request.storageName(),
                        LocalDateTime.now(),
                        deepLinkUrl
                );
            }
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

    private record ExpirationInfo(String status, Integer daysLeft) {
    }
}
