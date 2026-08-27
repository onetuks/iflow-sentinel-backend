package com.onetuks.iflow_sentinel.notification.repository;

import com.onetuks.iflow_sentinel.notification.domain.TenantNotificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TenantNotificationConfigRepository extends JpaRepository<TenantNotificationConfig, Long> {

    Optional<TenantNotificationConfig> findByTenantId(Long tenantId);

    @Query("SELECT c FROM TenantNotificationConfig c JOIN FETCH c.tenant WHERE c.isEnabled = true")
    List<TenantNotificationConfig> findAllByIsEnabledTrueWithTenant();

    void deleteByTenantId(Long tenantId);
}
