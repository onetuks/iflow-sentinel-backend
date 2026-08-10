package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackage;
import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackageRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.dto.ODataCollectionResponse;
import com.onetuks.iflow_sentinel.connector.dto.SapPackageDto;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** ART-001: 테넌트의 Integration Package 목록을 OData로 조회해 DB에 upsert한다. */
@Service
public class PackageSyncService {

    private final SapODataClient odataClient;
    private final IntegrationPackageRepository packageRepository;

    public PackageSyncService(SapODataClient odataClient, IntegrationPackageRepository packageRepository) {
        this.odataClient = odataClient;
        this.packageRepository = packageRepository;
    }

    public List<IntegrationPackage> syncPackages(Tenant tenant) {
        List<SapPackageDto> dtos = odataClient.getCollection(
                tenant,
                "/IntegrationPackages",
                new ParameterizedTypeReference<ODataCollectionResponse<SapPackageDto>>() {
                });

        List<IntegrationPackage> result = new ArrayList<>();
        for (SapPackageDto dto : dtos) {
            IntegrationPackage existing = packageRepository.findBySapPackageId(dto.Id()).orElse(null);
            if (existing != null) {
                existing.rename(dto.Name());
                result.add(packageRepository.save(existing));
            } else {
                IntegrationPackage created = IntegrationPackage.builder()
                        .tenant(tenant)
                        .sapPackageId(dto.Id())
                        .name(dto.Name())
                        .build();
                result.add(packageRepository.save(created));
            }
        }
        return result;
    }

    public void cleanOrphanPackages(Tenant tenant, Set<String> activeSapPackageIds) {
        List<IntegrationPackage> existingPackages = packageRepository.findByTenantId(tenant.getId());
        List<IntegrationPackage> orphans = existingPackages.stream()
                .filter(pkg -> !activeSapPackageIds.contains(pkg.getSapPackageId()))
                .toList();

        if (!orphans.isEmpty()) {
            packageRepository.deleteAll(orphans);
        }
    }
}
