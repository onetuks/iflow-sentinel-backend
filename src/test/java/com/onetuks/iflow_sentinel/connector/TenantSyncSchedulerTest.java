package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantPlatform;
import com.onetuks.iflow_sentinel.connector.dto.TenantResponse;
import com.onetuks.iflow_sentinel.connector.scheduler.TenantSyncScheduler;
import com.onetuks.iflow_sentinel.connector.service.TenantService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.LocalDate;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantSyncSchedulerTest {

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private TenantSyncScheduler scheduler;

    @Test
    void scheduleTenantSync_CallsSyncForEveryTenant() {
        TenantResponse t1 = new TenantResponse(1L, 1L, "Tenant 1", TenantPlatform.CLOUD_FOUNDRY, "https://t1.example.com", "https://t1.example.com/token", "c1", LocalDate.now(), null, null, null, null, "connected", 0);
        TenantResponse t2 = new TenantResponse(2L, 1L, "Tenant 2", TenantPlatform.CLOUD_FOUNDRY, "https://t2.example.com", "https://t2.example.com/token", "c2", LocalDate.now(), null, null, null, null, "connected", 0);

        when(tenantService.list(null)).thenReturn(List.of(t1, t2));

        scheduler.scheduleTenantSync();

        verify(tenantService).sync(1L);
        verify(tenantService).sync(2L);
    }

    @Test
    void scheduleTenantSync_ContinuesOnIndividualTenantFailure() {
        TenantResponse t1 = new TenantResponse(1L, 1L, "Tenant 1", TenantPlatform.CLOUD_FOUNDRY, "https://t1.example.com", "https://t1.example.com/token", "c1", LocalDate.now(), null, null, null, null, "connected", 0);
        TenantResponse t2 = new TenantResponse(2L, 1L, "Tenant 2", TenantPlatform.CLOUD_FOUNDRY, "https://t2.example.com", "https://t2.example.com/token", "c2", LocalDate.now(), null, null, null, null, "connected", 0);

        when(tenantService.list(null)).thenReturn(List.of(t1, t2));
        doThrow(new RuntimeException("Connection Failed")).when(tenantService).sync(1L);

        scheduler.scheduleTenantSync();

        verify(tenantService).sync(1L);
        verify(tenantService).sync(2L);
    }
}
