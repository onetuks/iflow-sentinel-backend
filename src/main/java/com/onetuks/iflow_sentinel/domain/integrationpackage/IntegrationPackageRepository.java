package com.onetuks.iflow_sentinel.domain.integrationpackage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntegrationPackageRepository extends JpaRepository<IntegrationPackage, Long> {

    List<IntegrationPackage> findByTenantId(Long tenantId);

    Optional<IntegrationPackage> findBySapPackageId(String sapPackageId);
}
