package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackageRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.connector.dto.IntegrationPackageResponse;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class IntegrationPackageService {

    private final TenantRepository tenantRepository;
    private final IntegrationPackageRepository packageRepository;
    private final PackageSyncService packageSyncService;

    public IntegrationPackageService(TenantRepository tenantRepository, IntegrationPackageRepository packageRepository,
            PackageSyncService packageSyncService) {
        this.tenantRepository = tenantRepository;
        this.packageRepository = packageRepository;
        this.packageSyncService = packageSyncService;
    }

    public List<IntegrationPackageResponse> sync(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("테넌트를 찾을 수 없습니다: " + tenantId));
        return packageSyncService.syncPackages(tenant).stream().map(IntegrationPackageResponse::from).toList();
    }

    public List<IntegrationPackageResponse> list(Long tenantId) {
        return packageRepository.findByTenantId(tenantId).stream().map(IntegrationPackageResponse::from).toList();
    }
}
