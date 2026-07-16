package com.onetuks.iflow_sentinel.connector.persistence;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantJpaRepository extends JpaRepository<Tenant, Long> {
}
