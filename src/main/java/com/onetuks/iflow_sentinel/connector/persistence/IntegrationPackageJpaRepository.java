package com.onetuks.iflow_sentinel.connector.persistence;

import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationPackageJpaRepository extends JpaRepository<IntegrationPackage, Long> {
}
