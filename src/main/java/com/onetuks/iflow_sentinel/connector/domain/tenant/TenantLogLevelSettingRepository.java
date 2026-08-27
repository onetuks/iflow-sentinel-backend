package com.onetuks.iflow_sentinel.connector.domain.tenant;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantLogLevelSettingRepository extends JpaRepository<TenantLogLevelSetting, Long> {

    Optional<TenantLogLevelSetting> findByTenantId(Long tenantId);
}
