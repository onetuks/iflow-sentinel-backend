package com.onetuks.iflow_sentinel.notification.domain;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_notification_configs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TenantNotificationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", unique = true, nullable = false)
    private Tenant tenant;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled;

    /**
     * 알림 수신자 이메일 목록 (쉼표 구분 문자열)
     * 예: "admin@company.com, ops@company.com"
     */
    @Column(columnDefinition = "TEXT")
    private String recipients;

    /**
     * 마지막으로 실패 리포트 발송 완료된 기준 일시 (워터마크)
     */
    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;

    @Builder
    public TenantNotificationConfig(Tenant tenant, boolean isEnabled, String recipients, LocalDateTime lastNotifiedAt) {
        this.tenant = tenant;
        this.isEnabled = isEnabled;
        this.recipients = recipients;
        this.lastNotifiedAt = lastNotifiedAt;
    }

    public void update(boolean isEnabled, String recipients) {
        this.isEnabled = isEnabled;
        this.recipients = recipients;
    }

    public void updateLastNotifiedAt(LocalDateTime lastNotifiedAt) {
        this.lastNotifiedAt = lastNotifiedAt;
    }
}
