package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackageRepository;
import com.onetuks.iflow_sentinel.connector.dto.IntegrationPackageResponse;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IntegrationPackageService {

    private final IntegrationPackageRepository packageRepository;

    public IntegrationPackageService(IntegrationPackageRepository packageRepository) {
        this.packageRepository = packageRepository;
    }

    public List<IntegrationPackageResponse> list(Long tenantId) {
        return packageRepository.findByTenantId(tenantId).stream().map(IntegrationPackageResponse::from).toList();
    }
}
