package com.onetuks.iflow_sentinel.notification.service;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.notification.domain.NotificationHistory;
import com.onetuks.iflow_sentinel.notification.domain.NotificationStatus;
import com.onetuks.iflow_sentinel.notification.domain.TenantNotificationConfig;
import com.onetuks.iflow_sentinel.notification.dto.NotificationHistoryResponse;
import com.onetuks.iflow_sentinel.notification.dto.TenantNotificationConfigRequest;
import com.onetuks.iflow_sentinel.notification.dto.TenantNotificationConfigResponse;
import com.onetuks.iflow_sentinel.notification.repository.NotificationHistoryRepository;
import com.onetuks.iflow_sentinel.notification.repository.TenantNotificationConfigRepository;
import com.onetuks.iflow_sentinel.reprocess.dto.MplFailureResponse;
import com.onetuks.iflow_sentinel.reprocess.service.MessageReprocessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class TenantFailureReportService {

    private static final Logger log = LoggerFactory.getLogger(TenantFailureReportService.class);

    private final TenantRepository tenantRepository;
    private final TenantNotificationConfigRepository configRepository;
    private final NotificationHistoryRepository historyRepository;
    private final MessageReprocessService messageReprocessService;
    private final EmailSenderService emailSenderService;
    private final EmailTemplateBuilder emailTemplateBuilder;

    public TenantFailureReportService(
            TenantRepository tenantRepository,
            TenantNotificationConfigRepository configRepository,
            NotificationHistoryRepository historyRepository,
            MessageReprocessService messageReprocessService,
            EmailSenderService emailSenderService,
            EmailTemplateBuilder emailTemplateBuilder) {
        this.tenantRepository = tenantRepository;
        this.configRepository = configRepository;
        this.historyRepository = historyRepository;
        this.messageReprocessService = messageReprocessService;
        this.emailSenderService = emailSenderService;
        this.emailTemplateBuilder = emailTemplateBuilder;
    }

    /**
     * 테넌트의 알림 설정 조회 (없으면 기본값 생성)
     */
    @Transactional
    public TenantNotificationConfigResponse getConfig(Long tenantId) {
        TenantNotificationConfig config = getOrCreateConfigEntity(tenantId);
        return TenantNotificationConfigResponse.from(config);
    }

    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    /**
     * 테넌트의 알림 설정 수정
     */
    @Transactional
    public TenantNotificationConfigResponse updateConfig(Long tenantId, TenantNotificationConfigRequest request) {
        TenantNotificationConfig config = getOrCreateConfigEntity(tenantId);

        // 수신자 이메일 형식 검증
        List<String> validatedRecipients = parseRecipients(request.recipients());
        if (Boolean.TRUE.equals(request.isEnabled()) && validatedRecipients.isEmpty()) {
            throw new IllegalArgumentException("알림을 활성화하려면 최소 1개 이상의 유효한 수신자 이메일이 필요합니다.");
        }

        config.update(Boolean.TRUE.equals(request.isEnabled()), String.join(", ", validatedRecipients));
        TenantNotificationConfig saved = configRepository.save(config);
        log.info("테넌트 알림 설정 갱신 완료. tenantId={}, isEnabled={}, recipients={}",
                tenantId, saved.isEnabled(), saved.getRecipients());
        return TenantNotificationConfigResponse.from(saved);
    }

    /**
     * 테넌트 알림 발송 이력 목록 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationHistoryResponse> getHistories(Long tenantId) {
        return historyRepository.findAllByTenantIdOrderBySentAtDesc(tenantId).stream()
                .map(NotificationHistoryResponse::from)
                .toList();
    }

    /**
     * 테스트 이메일 발송
     */
    public void sendTestEmail(Long tenantId, String targetEmail) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 테넌트입니다. ID=" + tenantId));

        String subject = String.format("[iFlow Sentinel] [%s] 테스트 메일 알림", tenant.getName());
        String htmlBody = emailTemplateBuilder.buildTestEmailHtml(tenant, targetEmail);

        emailSenderService.sendHtmlEmail(List.of(targetEmail), subject, htmlBody);
        log.info("테스트 이메일 발송 성공. tenantId={}, targetEmail={}", tenantId, targetEmail);
    }

    /**
     * 특정 테넌트의 실패 건을 조회하여 신규 건이 있으면 메일 리포트 발송
     *
     * @param tenantId 테넌트 ID
     * @param forceSend 신규 건 여부와 무관하게 조회된 실패 건에 대해 강제 발송할지 여부
     * @return 발송된 이력 정보 (발송 대상이 없는 경우 Optional.empty())
     */
    @Transactional
    public Optional<NotificationHistoryResponse> reportFailuresForTenant(Long tenantId, boolean forceSend) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 테넌트입니다. ID=" + tenantId));

        Optional<TenantNotificationConfig> optConfig = configRepository.findByTenantId(tenantId);
        if (optConfig.isEmpty() || (!forceSend && !optConfig.get().isEnabled())) {
            log.debug("테넌트 알림이 비활성화되어 있어 리포트를 건너뜁니다. tenantId={}", tenantId);
            return Optional.empty();
        }

        TenantNotificationConfig config = optConfig.get();
        List<String> recipients = parseRecipients(config.getRecipients());
        if (recipients.isEmpty()) {
            log.warn("테넌트 알림 수신자 목록이 비어 있어 발송할 수 없습니다. tenantId={}", tenantId);
            return Optional.empty();
        }

        // 1) 최근 실패 로그 조회 (최대 100건)
        List<MplFailureResponse> rawFailures;
        try {
            rawFailures = messageReprocessService.getMplFailures(tenantId, null, 100);
        } catch (Exception e) {
            log.error("테넌트 실패 로그 조회 실패. tenantId={}, error={}", tenantId, e.getMessage());
            return Optional.empty();
        }

        if (rawFailures == null || rawFailures.isEmpty()) {
            log.debug("테넌트에 감지된 실패 건이 없습니다. tenantId={}", tenantId);
            return Optional.empty();
        }

        // 2) 워터마크(lastNotifiedAt) 기준 신규 실패 건 필터링
        LocalDateTime watermark = config.getLastNotifiedAt();
        LocalDateTime effectiveWatermark = watermark != null ? watermark : LocalDateTime.now().minusHours(24);

        List<MplFailureResponse> newFailures = rawFailures.stream()
                .filter(f -> forceSend || (f.logStart() != null && f.logStart().isAfter(effectiveWatermark)))
                .sorted((a, b) -> {
                    if (a.logStart() == null && b.logStart() == null) return 0;
                    if (a.logStart() == null) return 1;
                    if (b.logStart() == null) return -1;
                    return b.logStart().compareTo(a.logStart());
                })
                .toList();

        if (newFailures.isEmpty()) {
            log.debug("기준 시각({}) 이후 발생한 신규 실패 건이 없습니다. tenantId={}", effectiveWatermark, tenantId);
            return Optional.empty();
        }

        // 3) 메일 제목 및 HTML 본문 렌더링
        LocalDateTime now = LocalDateTime.now();
        String subject = String.format("[iFlow Sentinel] [경고] %s 테넌트 실패 메시지 알림 (%d건)",
                tenant.getName(), newFailures.size());
        String htmlBody = emailTemplateBuilder.buildFailureReportHtml(tenant, newFailures, now);

        // 4) 메일 발송 및 결과 기록
        NotificationHistory history;
        try {
            emailSenderService.sendHtmlEmail(recipients, subject, htmlBody);

            LocalDateTime latestFailureTime = newFailures.stream()
                    .map(MplFailureResponse::logStart)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(now);

            config.updateLastNotifiedAt(latestFailureTime);
            configRepository.save(config);

            history = NotificationHistory.builder()
                    .tenant(tenant)
                    .sentAt(now)
                    .recipientCount(recipients.size())
                    .failureCount(newFailures.size())
                    .status(NotificationStatus.SUCCESS)
                    .subject(subject)
                    .errorMessage(null)
                    .build();

            log.info("테넌트 실패 리포트 발송 성공. tenantId={}, count={}, recipients={}",
                    tenantId, newFailures.size(), recipients.size());
        } catch (Exception e) {
            log.error("테넌트 실패 리포트 발송 실패. tenantId={}, error={}", tenantId, e.getMessage());

            history = NotificationHistory.builder()
                    .tenant(tenant)
                    .sentAt(now)
                    .recipientCount(recipients.size())
                    .failureCount(newFailures.size())
                    .status(NotificationStatus.FAILED)
                    .subject(subject)
                    .errorMessage(e.getMessage())
                    .build();
        }

        NotificationHistory savedHistory = historyRepository.save(history);
        return Optional.of(NotificationHistoryResponse.from(savedHistory));
    }

    private TenantNotificationConfig getOrCreateConfigEntity(Long tenantId) {
        return configRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    Tenant tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new NoSuchElementException("존재하지 않는 테넌트입니다. ID=" + tenantId));
                    TenantNotificationConfig newConfig = TenantNotificationConfig.builder()
                            .tenant(tenant)
                            .isEnabled(false)
                            .recipients("")
                            .lastNotifiedAt(null)
                            .build();
                    return configRepository.save(newConfig);
                });
    }

    private List<String> parseRecipients(String recipientsStr) {
        if (recipientsStr == null || recipientsStr.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(recipientsStr.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> {
                    boolean isValid = EMAIL_PATTERN.matcher(s).matches();
                    if (!isValid) {
                        log.warn("유효하지 않은 이메일 형식 무시: {}", s);
                    }
                    return isValid;
                })
                .toList();
    }
}
