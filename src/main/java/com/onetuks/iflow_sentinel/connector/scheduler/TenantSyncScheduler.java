package com.onetuks.iflow_sentinel.connector.scheduler;

import com.onetuks.iflow_sentinel.connector.dto.TenantResponse;
import com.onetuks.iflow_sentinel.connector.service.TenantService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 10분마다 모든 테넌트의 SAP IS 패키지 및 아티팩트 데이터를 주기적으로 자동 동기화(Reconciliation)하는 스케줄러.
 */
@Component
public class TenantSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TenantSyncScheduler.class);

    private final TenantService tenantService;

    public TenantSyncScheduler(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Scheduled(cron = "${app.tenant.sync-cron:0 */10 * * * *}")
    public void scheduleTenantSync() {
        log.info("Starting scheduled tenant synchronization...");
        List<TenantResponse> tenants = tenantService.list(null);

        for (TenantResponse tenant : tenants) {
            try {
                tenantService.sync(tenant.id());
                log.info("Successfully synced tenant: {} (ID: {})", tenant.name(), tenant.id());
            } catch (Exception e) {
                log.error("Failed to sync tenant: {} (ID: {}). Error: {}", tenant.name(), tenant.id(), e.getMessage());
            }
        }
        log.info("Completed scheduled tenant synchronization.");
    }
}
