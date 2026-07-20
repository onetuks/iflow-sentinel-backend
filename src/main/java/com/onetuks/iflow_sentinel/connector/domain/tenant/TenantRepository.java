package com.onetuks.iflow_sentinel.connector.domain.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    List<Tenant> findByProjectId(Long projectId);
}
