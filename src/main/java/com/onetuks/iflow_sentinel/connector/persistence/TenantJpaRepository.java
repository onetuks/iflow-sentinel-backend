package com.onetuks.iflow_sentinel.connector.persistence;

import com.onetuks.iflow_sentinel.domain.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantJpaRepository extends JpaRepository<Tenant, Long> {
}
