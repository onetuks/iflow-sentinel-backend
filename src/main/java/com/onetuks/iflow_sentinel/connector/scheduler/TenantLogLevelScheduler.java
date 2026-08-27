package com.onetuks.iflow_sentinel.connector.scheduler;

import com.onetuks.iflow_sentinel.connector.service.TenantLogLevelService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 10분마다 저장된 desired MPL 로그 레벨을 해당 테넌트의 배포된 아티팩트 전체에 재적용(Drift Correction)하는 스케줄러.
 */
@Component
public class TenantLogLevelScheduler {

    private static final Logger log = LoggerFactory.getLogger(TenantLogLevelScheduler.class);

    private final TenantLogLevelService logLevelService;

    public TenantLogLevelScheduler(TenantLogLevelService logLevelService) {
        this.logLevelService = logLevelService;
    }

    @Scheduled(cron = "${app.tenant.log-level-cron:0 */10 * * * *}")
    public void scheduleLogLevelReapply() {
        log.info("Starting scheduled tenant MPL log level reapplication...");
        List<Long> tenantIds = logLevelService.listTenantIdsWithSetting();

        for (Long tenantId : tenantIds) {
            try {
                logLevelService.reapplyTenantLogLevel(tenantId);
                log.info("Successfully reapplied log level for tenant ID: {}", tenantId);
            } catch (Exception e) {
                log.error("Failed to reapply log level for tenant ID: {}. Error: {}", tenantId, e.getMessage());
            }
        }
        log.info("Completed scheduled tenant MPL log level reapplication.");
    }
}
