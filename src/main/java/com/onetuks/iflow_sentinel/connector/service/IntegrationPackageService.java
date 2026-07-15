package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackage;
import com.onetuks.iflow_sentinel.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.dto.IntegrationPackageCreateRequest;
import com.onetuks.iflow_sentinel.connector.dto.IntegrationPackageUpdateRequest;
import com.onetuks.iflow_sentinel.connector.persistence.IntegrationPackageJpaRepository;
import com.onetuks.iflow_sentinel.connector.persistence.TenantJpaRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IntegrationPackageService {

    private final IntegrationPackageJpaRepository integrationPackageRepository;
    private final TenantJpaRepository tenantRepository;

    @Transactional
    public IntegrationPackage createIntegrationPackage(IntegrationPackageCreateRequest request) {
        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(NoSuchElementException::new);

        IntegrationPackage newPackage = IntegrationPackage.builder()
                .tenant(tenant)
                .sapPackageId(request.sapPackageId())
                .name(request.name())
                .build();

        return integrationPackageRepository.save(newPackage);
    }

    @Transactional
    public IntegrationPackage updateIntegrationPackage(Long id, IntegrationPackageUpdateRequest request) {
        IntegrationPackage integrationPackage = integrationPackageRepository.findById(id).orElseThrow(NoSuchElementException::new);
        // Add update logic here if Entity supports it
        return integrationPackageRepository.save(integrationPackage);
    }

    @Transactional(readOnly = true)
    public IntegrationPackage getIntegrationPackageById(Long id) {
        return integrationPackageRepository.findById(id).orElseThrow(NoSuchElementException::new);
    }

    @Transactional
    public void removeIntegrationPackage(Long id) {
        integrationPackageRepository.deleteById(id);
    }
}
