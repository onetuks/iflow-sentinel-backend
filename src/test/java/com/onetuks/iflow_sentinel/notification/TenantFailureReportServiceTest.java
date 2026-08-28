package com.onetuks.iflow_sentinel.notification;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantAuthType;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantPlatform;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.notification.domain.NotificationHistory;
import com.onetuks.iflow_sentinel.notification.domain.NotificationStatus;
import com.onetuks.iflow_sentinel.notification.domain.TenantNotificationConfig;
import com.onetuks.iflow_sentinel.notification.dto.NotificationHistoryResponse;
import com.onetuks.iflow_sentinel.notification.dto.TenantNotificationConfigRequest;
import com.onetuks.iflow_sentinel.notification.dto.TenantNotificationConfigResponse;
import com.onetuks.iflow_sentinel.notification.repository.NotificationHistoryRepository;
import com.onetuks.iflow_sentinel.notification.repository.TenantNotificationConfigRepository;
import com.onetuks.iflow_sentinel.notification.service.EmailSenderService;
import com.onetuks.iflow_sentinel.notification.service.EmailTemplateBuilder;
import com.onetuks.iflow_sentinel.notification.service.TenantFailureReportService;
import com.onetuks.iflow_sentinel.reprocess.dto.MplFailureResponse;
import com.onetuks.iflow_sentinel.reprocess.service.MessageReprocessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantFailureReportServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantNotificationConfigRepository configRepository;

    @Mock
    private NotificationHistoryRepository historyRepository;

    @Mock
    private MessageReprocessService messageReprocessService;

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private EmailTemplateBuilder emailTemplateBuilder;

    private TenantFailureReportService service;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        service = new TenantFailureReportService(
                tenantRepository,
                configRepository,
                historyRepository,
                messageReprocessService,
                emailSenderService,
                emailTemplateBuilder
        );

        tenant = Tenant.builder()
                .name("PROD_TENANT")
                .odataUrl("https://example.com/odata")
                .tokenUrl("https://example.com/oauth/token")
                .platformType(TenantPlatform.CLOUD_FOUNDRY)
                .authType(TenantAuthType.OAUTH2_CLIENT_CREDENTIALS)
                .clientId("client-id")
                .clientSecret("client-secret")
                .build();
        ReflectionTestUtils.setField(tenant, "id", 1L);
    }

    @Test
    @DisplayName("알림 설정이 없는 경우 기본 비활성화 설정 엔티티를 생성하고 반환한다")
    void getConfig_CreatesDefaultWhenNotFound() {
        // given
        when(configRepository.findByTenantId(1L)).thenReturn(Optional.empty());
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(configRepository.save(any(TenantNotificationConfig.class))).thenAnswer(invocation -> {
            TenantNotificationConfig c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 10L);
            return c;
        });

        // when
        TenantNotificationConfigResponse response = service.getConfig(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.tenantId()).isEqualTo(1L);
        assertThat(response.isEnabled()).isFalse();
        verify(configRepository).save(any(TenantNotificationConfig.class));
    }

    @Test
    @DisplayName("알림 설정을 활성화하고 수신자 목록을 수정할 수 있다")
    void updateConfig_Success() {
        // given
        TenantNotificationConfig config = TenantNotificationConfig.builder()
                .tenant(tenant)
                .isEnabled(false)
                .recipients("old@example.com")
                .build();
        ReflectionTestUtils.setField(config, "id", 10L);

        when(configRepository.findByTenantId(1L)).thenReturn(Optional.of(config));
        when(configRepository.save(any(TenantNotificationConfig.class))).thenAnswer(i -> i.getArgument(0));

        TenantNotificationConfigRequest request = new TenantNotificationConfigRequest(true, "admin1@test.com, admin2@test.com", 30);

        // when
        TenantNotificationConfigResponse response = service.updateConfig(1L, request);

        // then
        assertThat(response.isEnabled()).isTrue();
        assertThat(response.recipients()).isEqualTo("admin1@test.com, admin2@test.com");
        assertThat(response.intervalMinutes()).isEqualTo(30);
    }


    @Test
    @DisplayName("테스트 이메일을 정상적으로 발송한다")
    void sendTestEmail_Success() {
        // given
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(emailTemplateBuilder.buildTestEmailHtml(tenant, "dev@test.com")).thenReturn("<html>test</html>");

        // when
        service.sendTestEmail(1L, "dev@test.com");

        // then
        verify(emailSenderService).sendHtmlEmail(eq(List.of("dev@test.com")), anyString(), eq("<html>test</html>"));
    }

    @Test
    @DisplayName("알림이 비활성화된 상태이면 리포트 발송을 건너뛴다")
    void reportFailuresForTenant_WhenDisabled_ShouldSkip() {
        // given
        TenantNotificationConfig config = TenantNotificationConfig.builder()
                .tenant(tenant)
                .isEnabled(false)
                .recipients("admin@test.com")
                .build();

        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(configRepository.findByTenantId(1L)).thenReturn(Optional.of(config));

        // when
        Optional<NotificationHistoryResponse> result = service.reportFailuresForTenant(1L, false);

        // then
        assertThat(result).isEmpty();
        verify(emailSenderService, never()).sendHtmlEmail(any(), any(), any());
    }

    @Test
    @DisplayName("워터마크 이후 신규 실패 건이 존재하면 메일을 발송하고 워터마크와 히스토리를 갱신한다")
    void reportFailuresForTenant_WhenNewFailuresExist_ShouldSendEmailAndUpdateWatermark() {
        // given
        LocalDateTime lastNotified = LocalDateTime.of(2026, 8, 27, 10, 0, 0);
        LocalDateTime failureTime = LocalDateTime.of(2026, 8, 27, 10, 30, 0);

        TenantNotificationConfig config = TenantNotificationConfig.builder()
                .tenant(tenant)
                .isEnabled(true)
                .recipients("admin1@test.com, admin2@test.com")
                .lastNotifiedAt(lastNotified)
                .build();
        ReflectionTestUtils.setField(config, "id", 10L);

        MplFailureResponse failure = new MplFailureResponse(
                "MSG-1001",
                "CORR-01",
                "FAILED",
                "IF_ORDERS",
                "Orders Flow",
                failureTime,
                failureTime.plusSeconds(3),
                "STORE",
                "DATASTORE",
                "NORMAL",
                30,
                "Connection timeout"
        );

        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(configRepository.findByTenantId(1L)).thenReturn(Optional.of(config));
        when(messageReprocessService.getMplFailures(1L, null, 100)).thenReturn(List.of(failure));
        when(emailTemplateBuilder.buildFailureReportHtml(eq(tenant), anyList(), any())).thenReturn("<html>report</html>");
        when(historyRepository.save(any(NotificationHistory.class))).thenAnswer(i -> {
            NotificationHistory h = i.getArgument(0);
            ReflectionTestUtils.setField(h, "id", 100L);
            return h;
        });

        // when
        Optional<NotificationHistoryResponse> result = service.reportFailuresForTenant(1L, false);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().failureCount()).isEqualTo(1);
        assertThat(result.get().status()).isEqualTo(NotificationStatus.SUCCESS);

        verify(emailSenderService).sendHtmlEmail(
                eq(List.of("admin1@test.com", "admin2@test.com")),
                anyString(),
                eq("<html>report</html>")
        );
        assertThat(config.getLastNotifiedAt()).isEqualTo(failureTime);
    }

    @Test
    @DisplayName("이메일 발송 중 예외가 발생하면 FAILED 상태의 히스토리를 저장한다")
    void reportFailuresForTenant_WhenEmailFails_ShouldRecordFailedHistory() {
        // given
        LocalDateTime lastNotified = LocalDateTime.of(2026, 8, 27, 10, 0, 0);
        LocalDateTime failureTime = LocalDateTime.of(2026, 8, 27, 10, 30, 0);

        TenantNotificationConfig config = TenantNotificationConfig.builder()
                .tenant(tenant)
                .isEnabled(true)
                .recipients("admin@test.com")
                .lastNotifiedAt(lastNotified)
                .build();

        MplFailureResponse failure = new MplFailureResponse(
                "MSG-1001",
                "CORR-01",
                "FAILED",
                "IF_ORDERS",
                "Orders Flow",
                failureTime,
                failureTime.plusSeconds(3),
                "STORE",
                "DATASTORE",
                "NORMAL",
                30,
                "Connection timeout"
        );

        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(configRepository.findByTenantId(1L)).thenReturn(Optional.of(config));
        when(messageReprocessService.getMplFailures(1L, null, 100)).thenReturn(List.of(failure));
        when(emailTemplateBuilder.buildFailureReportHtml(eq(tenant), anyList(), any())).thenReturn("<html>report</html>");
        doThrow(new RuntimeException("SMTP Server Unreachable"))
                .when(emailSenderService).sendHtmlEmail(anyList(), anyString(), anyString());

        ArgumentCaptor<NotificationHistory> captor = ArgumentCaptor.forClass(NotificationHistory.class);
        when(historyRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        // when
        Optional<NotificationHistoryResponse> result = service.reportFailuresForTenant(1L, false);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getValue().getErrorMessage()).contains("SMTP Server Unreachable");
    }

    @Test
    @DisplayName("isDueForExecution - 알림 비활성화 또는 아직 주기가 되지 않은 경우 false 반환")
    void isDueForExecution_ReturnsFalse() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 28, 12, 0);

        // 1. 비활성화 상태
        TenantNotificationConfig disabledConfig = TenantNotificationConfig.builder()
                .tenant(tenant)
                .isEnabled(false)
                .intervalMinutes(15)
                .lastCheckedAt(null)
                .build();
        assertThat(service.isDueForExecution(disabledConfig, now)).isFalse();

        // 2. 최근 5분 전 점검 완료 (주기 15분)
        TenantNotificationConfig activeConfig = TenantNotificationConfig.builder()
                .tenant(tenant)
                .isEnabled(true)
                .intervalMinutes(15)
                .lastCheckedAt(now.minusMinutes(5))
                .build();
        assertThat(service.isDueForExecution(activeConfig, now)).isFalse();
    }

    @Test
    @DisplayName("isDueForExecution - 최초 점검(lastCheckedAt == null)이거나 주기가 도래한 경우 true 반환")
    void isDueForExecution_ReturnsTrue() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 28, 12, 0);

        // 1. 최초 점검
        TenantNotificationConfig firstTimeConfig = TenantNotificationConfig.builder()
                .tenant(tenant)
                .isEnabled(true)
                .intervalMinutes(15)
                .lastCheckedAt(null)
                .build();
        assertThat(service.isDueForExecution(firstTimeConfig, now)).isTrue();

        // 2. 주기(15분) 경과 (20분 전 점검)
        TenantNotificationConfig dueConfig = TenantNotificationConfig.builder()
                .tenant(tenant)
                .isEnabled(true)
                .intervalMinutes(15)
                .lastCheckedAt(now.minusMinutes(20))
                .build();
        assertThat(service.isDueForExecution(dueConfig, now)).isTrue();
    }
}

