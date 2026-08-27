package com.onetuks.iflow_sentinel.notification.controller;

import com.onetuks.iflow_sentinel.notification.dto.NotificationHistoryResponse;
import com.onetuks.iflow_sentinel.notification.dto.TenantNotificationConfigRequest;
import com.onetuks.iflow_sentinel.notification.dto.TenantNotificationConfigResponse;
import com.onetuks.iflow_sentinel.notification.dto.TestEmailRequest;
import com.onetuks.iflow_sentinel.notification.service.TenantFailureReportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/notifications")
public class TenantNotificationController {

    private final TenantFailureReportService reportService;

    public TenantNotificationController(TenantFailureReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<TenantNotificationConfigResponse> getConfig(@PathVariable Long tenantId) {
        TenantNotificationConfigResponse config = reportService.getConfig(tenantId);
        return ResponseEntity.ok(config);
    }

    @PutMapping
    public ResponseEntity<TenantNotificationConfigResponse> updateConfig(
            @PathVariable Long tenantId,
            @Valid @RequestBody TenantNotificationConfigRequest request) {
        TenantNotificationConfigResponse updated = reportService.updateConfig(tenantId, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/test-mail")
    public ResponseEntity<Void> sendTestEmail(
            @PathVariable Long tenantId,
            @Valid @RequestBody TestEmailRequest request) {
        reportService.sendTestEmail(tenantId, request.targetEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-report")
    public ResponseEntity<NotificationHistoryResponse> sendReportNow(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "false") boolean force) {
        return reportService.reportFailuresForTenant(tenantId, force)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/histories")
    public ResponseEntity<List<NotificationHistoryResponse>> getHistories(@PathVariable Long tenantId) {
        List<NotificationHistoryResponse> histories = reportService.getHistories(tenantId);
        return ResponseEntity.ok(histories);
    }
}
