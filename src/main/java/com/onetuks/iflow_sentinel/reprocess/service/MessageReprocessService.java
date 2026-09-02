package com.onetuks.iflow_sentinel.reprocess.service;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.connector.dto.ODataCollectionResponse;
import com.onetuks.iflow_sentinel.exception.ConnectorException;
import com.onetuks.iflow_sentinel.reprocess.domain.ProtocolType;
import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessHistory;
import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessHistoryRepository;
import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessStatus;
import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessSupportType;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageType;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageBodyResponse;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageReprocessRequest;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageReprocessResult;
import com.onetuks.iflow_sentinel.reprocess.dto.MplFailureResponse;
import com.onetuks.iflow_sentinel.reprocess.dto.ReprocessHistoryResponse;
import com.onetuks.iflow_sentinel.reprocess.dto.SapDataStoreEntryDto;
import com.onetuks.iflow_sentinel.reprocess.dto.SapMplLogDto;
import com.onetuks.iflow_sentinel.reprocess.dto.StorageMappingDto;
import com.onetuks.iflow_sentinel.connector.dto.SapRuntimeArtifactDto;
import com.onetuks.iflow_sentinel.reprocess.dto.SapEntryPointDto;
import com.onetuks.iflow_sentinel.reprocess.dto.SapServiceEndpointDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class MessageReprocessService {

    private static final Logger log = LoggerFactory.getLogger(MessageReprocessService.class);

    private final ArtifactRepository artifactRepository;
    private final TenantRepository tenantRepository;
    private final SapODataClient sapODataClient;
    private final StorageMappingService storageMappingService;
    private final ReprocessHistoryRepository reprocessHistoryRepository;
    private final List<MessageSender> messageSenders;

    public MessageReprocessService(ArtifactRepository artifactRepository,
                                   TenantRepository tenantRepository,
                                   SapODataClient sapODataClient,
                                   StorageMappingService storageMappingService,
                                   ReprocessHistoryRepository reprocessHistoryRepository,
                                   List<MessageSender> messageSenders) {
        this.artifactRepository = artifactRepository;
        this.tenantRepository = tenantRepository;
        this.sapODataClient = sapODataClient;
        this.storageMappingService = storageMappingService;
        this.reprocessHistoryRepository = reprocessHistoryRepository;
        this.messageSenders = messageSenders;
    }

    @Transactional(readOnly = true)
    public ReprocessSupportType getReprocessSupportType(String artifactId) {
        if (artifactId == null || artifactId.isBlank()) {
            return ReprocessSupportType.NONE;
        }
        return artifactRepository.findById(artifactId)
                .map(Artifact::getReprocessSupportType)
                .orElse(ReprocessSupportType.NONE);
    }

    @Transactional(readOnly = true)
    public List<MplFailureResponse> getMplFailures(Long tenantId, String artifactIdStr, int top) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 테넌트입니다. ID=" + tenantId));

        String sapArtifactId = null;
        String artifactName = null;

        if (artifactIdStr != null && !artifactIdStr.isBlank()) {
            Optional<Artifact> optArtifact = artifactRepository.findById(artifactIdStr);
            if (optArtifact.isPresent()) {
                Artifact artifact = optArtifact.get();
                sapArtifactId = artifact.getSapArtifactId();
                artifactName = artifact.getName();
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
                    .filter(logDto -> this.isTargetArtifact(logDto, targetId))
                    .toList();
        }

        // 2) Status (FAILED, ESCALATED, CANCELLED), SubStatus, CustomStatus (소문자 'fail', 'err', 'cancel' 포함) 에러 건 검증
        rawLogs = rawLogs.stream()
                .filter(logDto -> this.isErrorStatus(logDto.status(), logDto.subStatus(), logDto.customStatus()))
                .toList();

        List<MplFailureResponse> result = new ArrayList<>();

        // 저장소 정보 조회 (sapArtifactId가 존재하는 경우 매핑 조회)
        Optional<StorageMappingDto> dsMapping = sapArtifactId != null
                ? storageMappingService.getStorageMapping(tenantId, sapArtifactId, StorageType.DATASTORE)
                : Optional.empty();
        Optional<StorageMappingDto> jmsMapping = sapArtifactId != null
                ? storageMappingService.getStorageMapping(tenantId, sapArtifactId, StorageType.JMS)
                : Optional.empty();

        String storageName = dsMapping.filter(m -> m != null && m.storageName() != null)
                .map(StorageMappingDto::storageName)
                .orElseGet(() -> jmsMapping.filter(m -> m != null && m.storageName() != null)
                        .map(StorageMappingDto::storageName)
                        .orElse("N/A"));
        String storageType = dsMapping.isPresent() ? "DATASTORE" : (jmsMapping.isPresent() ? "JMS" : "UNKNOWN");
        Integer expireDays = dsMapping.filter(m -> m != null && m.expireDays() != null)
                .map(StorageMappingDto::expireDays)
                .orElse(null);

        for (SapMplLogDto dto : rawLogs) {
            LocalDateTime start = parseODataDateTime(dto.logStart());
            LocalDateTime end = parseODataDateTime(dto.logEnd());

            ExpirationInfo expInfo = calculateExpiration(start, expireDays);

            String errorDetail = dto.getEffectiveErrorDetail();
            if (errorDetail == null || errorDetail.isBlank()) {
                // $expand가 거부되는 SAP OData 501 회피를 위해 개별 평문 에러 조회 호출
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
    public MessageBodyResponse getMessageBody(Long tenantId, String artifactId, String messageId,
                                              StorageType storageType) {
        return getMessageBody(tenantId, artifactId, messageId, storageType, null);
    }

    @Transactional(readOnly = true)
    public MessageBodyResponse getMessageBody(Long tenantId, String artifactId, String messageId, StorageType storageType,
                                              String requestedStorageName) {
        log.info("메시지 바디 조회 요청 시작 - tenantId: {}, artifactId: {}, messageId: {}, storageType: {}, requestedStorageName: {}",
                tenantId, artifactId, messageId, storageType, requestedStorageName);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 테넌트입니다. ID=" + tenantId));

        String storageName = requestedStorageName;
        Integer expireDays = 30;

        Optional<StorageMappingDto> mapping = (artifactId != null && !artifactId.isBlank())
                ? storageMappingService.getStorageMapping(tenantId, artifactId, storageType)
                : Optional.empty();
        if (mapping.isPresent()) {
            if (storageName == null || storageName.isBlank()) {
                storageName = mapping.get().storageName();
            }
            expireDays = mapping.get().expireDays();
        }

        if (storageName == null || storageName.isBlank()) {
            // Artifact 이름 기반 순수 명칭 fallback
            if (artifactId != null && !artifactId.isBlank()) {
                storageName = artifactRepository.findById(artifactId)
                        .map(Artifact::getName)
                        .orElse(null);
            }
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
     * MessageId로 DataStoreEntries에서 엔트리를 찾는다.
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

        Artifact artifact = null;
        String artifactName = null;
        if (request.artifactId() != null) {
            artifact = artifactRepository.findById(request.artifactId()).orElse(null);
            if (artifact != null) {
                artifactName = artifact.getName();
            }
        }

        String reprocessedBy = (request.reprocessedBy() != null && !request.reprocessedBy().isBlank())
                ? request.reprocessedBy()
                : "SYSTEM";

        String deepLinkUrl = buildSapDeepLinkUrl(tenant, request.storageType(), request.storageName());
        LocalDateTime now = LocalDateTime.now();

        // 1. DataStore 메시지 재처리 또는 페이로드가 명시된 재처리 요청: 인터페이스 직접 호출 실행
        if (request.storageType() == StorageType.DATASTORE || (request.payload() != null && !request.payload().isBlank())) {
            ResolvedEndpoint resolvedEndpoint = resolveInterfaceEndpoint(tenant, artifact, request.endpointUrl());
            String targetEndpointUrl = resolvedEndpoint.url();
            ProtocolType protocolType = request.protocolType() != null
                    ? request.protocolType()
                    : (resolvedEndpoint.protocolType() != null ? resolvedEndpoint.protocolType() : ProtocolType.HTTP);
            log.info("재처리 호출 프로토콜 결정 - messageId: {}, protocolType: {} (요청 명시: {}, SAP 자동판별: {})",
                    request.messageId(), protocolType, request.protocolType(), resolvedEndpoint.protocolType());

            // ProcessDirect는 같은 테넌트 내부 iFlow 간 전용 어댑터라 외부에서 직접 재호출할 수 없다.
            // 호출을 시도조차 하지 않고, 잘못된 프로토콜 호출로 인터페이스 측 에러가 발생하는 것을 사전에 막는다.
            if (protocolType == ProtocolType.PROCESS_DIRECT) {
                String processDirectMessage = "이 인터페이스는 ProcessDirect(내부 전용) 어댑터로 구성되어 있어 "
                        + "외부에서 직접 재처리 호출을 할 수 없습니다. 이 메시지를 전달한 상위(진입) iFlow의 MPL 로그에서 재처리해주세요.";

                ReprocessHistory history = ReprocessHistory.builder()
                        .tenantId(request.tenantId())
                        .tenantName(tenant.getName())
                        .artifactId(request.artifactId())
                        .artifactName(artifactName)
                        .messageId(request.messageId())
                        .storageType(request.storageType())
                        .storageName(request.storageName())
                        .status(ReprocessStatus.FAILED)
                        .statusMessage(processDirectMessage)
                        .reprocessedAt(now)
                        .reprocessedBy(reprocessedBy)
                        .deepLinkUrl(deepLinkUrl)
                        .endpointUrl(targetEndpointUrl)
                        .httpStatusCode(null)
                        .build();
                ReprocessHistory savedHistory = reprocessHistoryRepository.save(history);

                log.warn("ProcessDirect 어댑터 인터페이스로 재처리 요청됨 - 직접 호출 불가 안내 처리 - messageId: {}, artifactId: {}",
                        request.messageId(), request.artifactId());

                return new MessageReprocessResult(
                        savedHistory.getId(),
                        request.messageId(),
                        false,
                        processDirectMessage,
                        request.storageType().name(),
                        request.storageName(),
                        now,
                        deepLinkUrl,
                        targetEndpointUrl,
                        null);
            }

            // 호출 가능한 인터페이스 URL이 존재하지 않는 경우 (미배포 또는 노출 URL 없음)
            if (targetEndpointUrl == null || targetEndpointUrl.isBlank()) {
                String noUrlMessage = "호출 가능한 인터페이스 엔드포인트 URL을 찾을 수 없습니다. SAP에 해당 아티팩트가 배포되어 활성화되어 있는지 확인해주세요.";
                int noUrlStatusCode = 404;

                ReprocessHistory history = ReprocessHistory.builder()
                        .tenantId(request.tenantId())
                        .tenantName(tenant.getName())
                        .artifactId(request.artifactId())
                        .artifactName(artifactName)
                        .messageId(request.messageId())
                        .storageType(request.storageType())
                        .storageName(request.storageName())
                        .status(ReprocessStatus.FAILED)
                        .statusMessage(noUrlMessage)
                        .reprocessedAt(now)
                        .reprocessedBy(reprocessedBy)
                        .deepLinkUrl(deepLinkUrl)
                        .endpointUrl(null)
                        .httpStatusCode(noUrlStatusCode)
                        .build();
                ReprocessHistory savedHistory = reprocessHistoryRepository.save(history);

                log.warn("인터페이스 엔드포인트 URL 미존재로 재처리 실패 처리 - messageId: {}, artifactId: {}",
                        request.messageId(), request.artifactId());

                return new MessageReprocessResult(
                        savedHistory.getId(),
                        request.messageId(),
                        false,
                        noUrlMessage,
                        request.storageType().name(),
                        request.storageName(),
                        now,
                        deepLinkUrl,
                        null,
                        noUrlStatusCode);
            }

            try {
                // 페이로드 확보
                String payload = request.payload();
                if (payload == null || payload.isBlank()) {
                    payload = fetchBinaryPayload(tenant, request.messageId());
                }

                MessageSender messageSender = resolveMessageSender(protocolType);
                ResponseEntity<String> response = messageSender.send(
                        tenant, targetEndpointUrl, payload, request.soapAction()
                );

                int statusCode = response.getStatusCode().value();
                String successMessage = "인터페이스 직접 호출 재처리 성공 [HTTP " + statusCode + "] - Endpoint: " + targetEndpointUrl;

                ReprocessHistory history = ReprocessHistory.builder()
                        .tenantId(request.tenantId())
                        .tenantName(tenant.getName())
                        .artifactId(request.artifactId())
                        .artifactName(artifactName)
                        .messageId(request.messageId())
                        .storageType(request.storageType())
                        .storageName(request.storageName())
                        .status(ReprocessStatus.SUCCESS)
                        .statusMessage(successMessage)
                        .reprocessedAt(now)
                        .reprocessedBy(reprocessedBy)
                        .deepLinkUrl(deepLinkUrl)
                        .endpointUrl(targetEndpointUrl)
                        .httpStatusCode(statusCode)
                        .build();
                ReprocessHistory savedHistory = reprocessHistoryRepository.save(history);

                log.info("인터페이스 직접 호출 메시지 재처리 성공 및 히스토리 저장 완료 - historyId: {}, messageId: {}, endpoint: {}",
                        savedHistory.getId(), request.messageId(), targetEndpointUrl);

                return new MessageReprocessResult(
                        savedHistory.getId(),
                        request.messageId(),
                        true,
                        successMessage,
                        request.storageType().name(),
                        request.storageName(),
                        now,
                        deepLinkUrl,
                        targetEndpointUrl,
                        statusCode);
            } catch (Exception e) {
                String failureMessage = "인터페이스 직접 호출 재처리 실패: " + e.getMessage();
                Integer httpStatus = (e instanceof ConnectorException ce) ? ce.getStatusCode() : null;
                log.error("인터페이스 직접 호출 재처리 실패 - messageId: {}, endpoint: {}, 사유: {}",
                        request.messageId(), targetEndpointUrl, failureMessage, e);

                ReprocessHistory history = ReprocessHistory.builder()
                        .tenantId(request.tenantId())
                        .tenantName(tenant.getName())
                        .artifactId(request.artifactId())
                        .artifactName(artifactName)
                        .messageId(request.messageId())
                        .storageType(request.storageType())
                        .storageName(request.storageName())
                        .status(ReprocessStatus.FAILED)
                        .statusMessage(failureMessage)
                        .reprocessedAt(now)
                        .reprocessedBy(reprocessedBy)
                        .deepLinkUrl(deepLinkUrl)
                        .endpointUrl(targetEndpointUrl)
                        .httpStatusCode(httpStatus)
                        .build();
                ReprocessHistory savedHistory = reprocessHistoryRepository.save(history);

                return new MessageReprocessResult(
                        savedHistory.getId(),
                        request.messageId(),
                        false,
                        failureMessage,
                        request.storageType().name(),
                        request.storageName(),
                        now,
                        deepLinkUrl,
                        targetEndpointUrl,
                        httpStatus);
            }
        } else {
            // 페이로드가 명시되지 않은 순수 JMS 큐 메시지는 Web UI 매핑 안내 반환 및 히스토리 기록
            String jmsMessage = "JMS 큐 (" + request.storageName() + ") 메시지 재처리를 위해 SAP IS Manage Queues 바로가기 링크가 생성되었습니다.";
            ReprocessHistory history = ReprocessHistory.builder()
                    .tenantId(request.tenantId())
                    .tenantName(tenant.getName())
                    .artifactId(request.artifactId())
                    .artifactName(artifactName)
                    .messageId(request.messageId())
                    .storageType(request.storageType())
                    .storageName(request.storageName())
                    .status(ReprocessStatus.SUCCESS)
                    .statusMessage(jmsMessage)
                    .reprocessedAt(now)
                    .reprocessedBy(reprocessedBy)
                    .deepLinkUrl(deepLinkUrl)
                    .endpointUrl(null)
                    .httpStatusCode(null)
                    .build();
            ReprocessHistory savedHistory = reprocessHistoryRepository.save(history);

            log.info("JMS 메시지 재처리 가이드 생성 및 히스토리 저장 완료 - historyId: {}, messageId: {}",
                    savedHistory.getId(), request.messageId());

            return new MessageReprocessResult(
                    savedHistory.getId(),
                    request.messageId(),
                    true,
                    jmsMessage,
                    request.storageType().name(),
                    request.storageName(),
                    now,
                    deepLinkUrl,
                    null,
                    null);
        }
    }

    @Transactional(readOnly = true)
    public List<ReprocessHistoryResponse> getReprocessHistories(Long tenantId, String artifactId, String messageId, ReprocessStatus status) {
        return reprocessHistoryRepository.searchHistories(tenantId, artifactId, messageId, status)
                .stream()
                .map(ReprocessHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReprocessHistoryResponse getReprocessHistory(Long id) {
        return reprocessHistoryRepository.findById(id)
                .map(ReprocessHistoryResponse::from)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 재처리 이력입니다. ID=" + id));
    }

    @Transactional
    public void deleteReprocessHistory(Long id) {
        if (!reprocessHistoryRepository.existsById(id)) {
            throw new NoSuchElementException("존재하지 않는 재처리 이력입니다. ID=" + id);
        }
        reprocessHistoryRepository.deleteById(id);
    }

    /**
     * iFlow 인터페이스의 호출 엔드포인트 URL을 탐색한다. 프로토콜 자동판별이 필요 없는 호출부를 위한 얇은 래퍼.
     * @see #resolveInterfaceEndpoint(Tenant, Artifact, String)
     */
    public String resolveInterfaceEndpointUrl(Tenant tenant, Artifact artifact, String requestedEndpointUrl) {
        return resolveInterfaceEndpoint(tenant, artifact, requestedEndpointUrl).url();
    }

    /**
     * iFlow 인터페이스의 호출 엔드포인트 URL과, SAP OData {@code /ServiceEndpoints}의 {@code Protocol}
     * 필드로부터 판별한 호출 프로토콜을 함께 탐색한다.
     * 1) 요청에 직접 전달된 endpointUrl (SAP 메타데이터가 없으므로 protocolType은 null)
     * 2) SAP OData /ServiceEndpoints?$filter=Name eq '{name}'&$expand=EntryPoints 직접 조회 (1순위)
     * 3) SAP OData /IntegrationRuntimeArtifacts('{sapArtifactId}')/ServiceEndpoints 직접 조회 (2순위)
     * 4) SAP OData 전체 /ServiceEndpoints 목록 조회 매칭 (3순위)
     * 5) SAP OData 배포된 런타임 아티팩트 목록(/IntegrationRuntimeArtifacts) 탐색 후 해당 엔드포인트 조회 (4순위)
     * 6) 탐색 실패 시 임의 가상 URL을 생성하지 않고 빈 결과 반환
     */
    private ResolvedEndpoint resolveInterfaceEndpoint(Tenant tenant, Artifact artifact, String requestedEndpointUrl) {
        if (requestedEndpointUrl != null && !requestedEndpointUrl.isBlank()) {
            return new ResolvedEndpoint(requestedEndpointUrl.trim(), null);
        }

        if (artifact == null) {
            return ResolvedEndpoint.EMPTY;
        }

        String sapArtId = artifact.getSapArtifactId();
        String artName = artifact.getName();

        // 1. /ServiceEndpoints?$filter=Name eq '{name}'&$expand=EntryPoints 직접 조회 (1순위: artName 및 sapArtId)
        List<String> candidateNames = new ArrayList<>();
        if (artName != null && !artName.isBlank()) {
            candidateNames.add(artName);
        }
        if (sapArtId != null && !sapArtId.isBlank() && !sapArtId.equalsIgnoreCase(artName)) {
            candidateNames.add(sapArtId);
        }

        for (String candidateName : candidateNames) {
            try {
                List<SapServiceEndpointDto> filteredEndpoints =
                        sapODataClient.getServiceEndpointsByName(tenant, candidateName);
                if (filteredEndpoints != null && !filteredEndpoints.isEmpty()) {
                    for (var ep : filteredEndpoints) {
                        String resolvedUrl = ep.resolveUrl();
                        if (resolvedUrl != null && !resolvedUrl.isBlank()) {
                            log.info("ServiceEndpoints 필터 조회($filter=Name eq '{}')를 통해 URL 확보 성공: {} (Protocol: {})",
                                    candidateName, resolvedUrl, ep.Protocol());
                            return new ResolvedEndpoint(resolvedUrl, mapProtocol(ep.Protocol()));
                        }
                        // EntryPoints 별도 호출 폴백
                        if (ep.Id() != null && !ep.Id().isBlank()) {
                            List<SapEntryPointDto> entryPoints = sapODataClient.getEntryPointsForServiceEndpoint(tenant, ep.Id());
                            if (entryPoints != null && !entryPoints.isEmpty()) {
                                for (var entryPoint : entryPoints) {
                                    if (entryPoint.Url() != null && !entryPoint.Url().isBlank()) {
                                        log.info("ServiceEndpoints EntryPoints 조회를 통해 URL 확보 성공: {} (Protocol: {})",
                                                entryPoint.Url(), ep.Protocol());
                                        return new ResolvedEndpoint(entryPoint.Url().trim(), mapProtocol(ep.Protocol()));
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("ServiceEndpoints Name 필터({}) 조회 실패: {}", candidateName, e.getMessage());
            }
        }

        // 2. 배포된 런타임 아티팩트에서 직접 ServiceEndpoints 조회 시도 (2순위: sapArtifactId)
        if (sapArtId != null && !sapArtId.isBlank()) {
            try {
                List<SapServiceEndpointDto> runtimeEndpoints =
                        sapODataClient.getServiceEndpointsForRuntimeArtifact(tenant, sapArtId);
                if (runtimeEndpoints != null && !runtimeEndpoints.isEmpty()) {
                    for (var ep : runtimeEndpoints) {
                        String resolvedUrl = ep.resolveUrl();
                        if (resolvedUrl != null && !resolvedUrl.isBlank()) {
                            return new ResolvedEndpoint(resolvedUrl, mapProtocol(ep.Protocol()));
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("특정 Runtime Artifact ID({}) 기반 ServiceEndpoints 조회 실패 (전체 목록 탐색 진행): {}", sapArtId, e.getMessage());
            }
        }

        // 3. 전체 배포된 ServiceEndpoints 목록을 조회하여 이름/ID 매칭 탐색 (3순위)
        try {
            List<SapServiceEndpointDto> endpoints =
                    sapODataClient.getServiceEndpoints(tenant);
            if (endpoints != null && !endpoints.isEmpty()) {
                for (var ep : endpoints) {
                    if (ep.Name() != null) {
                        if (ep.Name().equalsIgnoreCase(sapArtId) || ep.Name().equalsIgnoreCase(artName)
                                || (sapArtId != null && ep.Name().contains(sapArtId))
                                || (artName != null && ep.Name().contains(artName))) {
                            String resolvedUrl = ep.resolveUrl();
                            if (resolvedUrl != null && !resolvedUrl.isBlank()) {
                                return new ResolvedEndpoint(resolvedUrl, mapProtocol(ep.Protocol()));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("SAP 전체 ServiceEndpoints 목록 조회 실패: {}", e.getMessage());
        }

        // 4. 배포된 런타임 아티팩트 목록(/IntegrationRuntimeArtifacts)에서 이름으로 매칭 후 해당 ID로 조회 시도 (4순위)
        try {
            List<SapRuntimeArtifactDto> runtimeArtifacts =
                    sapODataClient.getRuntimeArtifacts(tenant);
            if (runtimeArtifacts != null) {
                for (var rArt : runtimeArtifacts) {
                    if ((sapArtId != null && sapArtId.equalsIgnoreCase(rArt.Id()))
                            || (artName != null && artName.equalsIgnoreCase(rArt.Name()))
                            || (sapArtId != null && rArt.Id() != null && rArt.Id().contains(sapArtId))
                            || (artName != null && rArt.Name() != null && rArt.Name().contains(artName))) {
                        List<SapServiceEndpointDto> epList =
                                sapODataClient.getServiceEndpointsForRuntimeArtifact(tenant, rArt.Id());
                        if (epList != null && !epList.isEmpty()) {
                            for (var ep : epList) {
                                String resolvedUrl = ep.resolveUrl();
                                if (resolvedUrl != null && !resolvedUrl.isBlank()) {
                                    return new ResolvedEndpoint(resolvedUrl, mapProtocol(ep.Protocol()));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("배포된 Runtime Artifact 탐색 및 ServiceEndpoints 조회 실패: {}", e.getMessage());
        }

        // 5. 어떤 방법으로도 런타임 엔드포인트를 찾을 수 없는 경우 빈 결과 반환
        return ResolvedEndpoint.EMPTY;
    }

    /**
     * SAP OData {@code ServiceEndpoints.Protocol} 원본 값(예: "HTTP", "SOAP", "SOAP 1.1", "REST",
     * "ProcessDirect")을 현재 지원하는 {@link ProtocolType}으로 매핑한다. "SOAP"을 포함하면 SOAP,
     * "ProcessDirect"를 포함하면 PROCESS_DIRECT, 그 외(HTTP/REST/HTTPS 등)는 HTTP로 취급하고,
     * 값이 없으면(null) 호출부에서 명시적 지정 또는 기본값(HTTP)으로 폴백하도록 null을 반환한다.
     */
    private ProtocolType mapProtocol(String rawProtocol) {
        if (rawProtocol == null || rawProtocol.isBlank()) {
            return null;
        }
        String normalized = rawProtocol.toUpperCase().replace(" ", "").replace("-", "").replace("_", "");
        if (normalized.contains("SOAP")) {
            return ProtocolType.SOAP;
        }
        if (normalized.contains("PROCESSDIRECT")) {
            return ProtocolType.PROCESS_DIRECT;
        }
        return ProtocolType.HTTP;
    }

    private record ResolvedEndpoint(String url, ProtocolType protocolType) {
        private static final ResolvedEndpoint EMPTY = new ResolvedEndpoint(null, null);
    }

    private MessageSender resolveMessageSender(ProtocolType protocolType) {
        return messageSenders.stream()
                .filter(sender -> sender.supports(protocolType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("지원하지 않는 프로토콜입니다: " + protocolType));
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
        String baseUrl = tenant.getApiUrl() != null ? tenant.getApiUrl()
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
