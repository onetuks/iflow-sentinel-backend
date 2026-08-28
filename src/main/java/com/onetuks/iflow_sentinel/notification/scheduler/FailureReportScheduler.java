package com.onetuks.iflow_sentinel.notification.scheduler;

import com.onetuks.iflow_sentinel.notification.domain.TenantNotificationConfig;
import com.onetuks.iflow_sentinel.notification.repository.TenantNotificationConfigRepository;
import com.onetuks.iflow_sentinel.notification.service.TenantFailureReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주기적으로 활성화된 테넌트의 실패 메시지를 검사하여 담당자에게 이메일 리포트를 발송하는 스케줄러
 */
@Component
public class FailureReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(FailureReportScheduler.class);

    private final TenantNotificationConfigRepository configRepository;
    private final TenantFailureReportService failureReportService;

    public FailureReportScheduler(
            TenantNotificationConfigRepository configRepository,
            TenantFailureReportService failureReportService) {
        this.configRepository = configRepository;
        this.failureReportService = failureReportService;
    }

    @Scheduled(cron = "${app.notification.cron:0 * * * * *}")
    public void scheduleFailureReporting() {
        List<TenantNotificationConfig> activeConfigs = configRepository.findAllByIsEnabledTrueWithTenant();
        LocalDateTime now = LocalDateTime.now();

        for (TenantNotificationConfig config : activeConfigs) {
            if (!failureReportService.isDueForExecution(config, now)) {
                continue;
            }

            Long tenantId = config.getTenant().getId();
            String tenantName = config.getTenant().getName();
            try {
                failureReportService.reportFailuresForTenant(tenantId, false);
            } catch (Exception e) {
                log.error("Failed to run failure report for tenant: {} (ID: {}). Error: {}",
                        tenantName, tenantId, e.getMessage(), e);
            }
        }
    }
}

