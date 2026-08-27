package com.onetuks.iflow_sentinel.notification.repository;

import com.onetuks.iflow_sentinel.notification.domain.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    List<NotificationHistory> findAllByTenantIdOrderBySentAtDesc(Long tenantId);
}
