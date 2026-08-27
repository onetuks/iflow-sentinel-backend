package com.onetuks.iflow_sentinel.notification.dto;

import com.onetuks.iflow_sentinel.notification.domain.NotificationHistory;
import com.onetuks.iflow_sentinel.notification.domain.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationHistoryResponse(
        Long id,
        Long tenantId,
        String tenantName,
        LocalDateTime sentAt,
        int recipientCount,
        int failureCount,
        NotificationStatus status,
        String subject,
        String errorMessage
) {
    public static NotificationHistoryResponse from(NotificationHistory history) {
        return new NotificationHistoryResponse(
                history.getId(),
                history.getTenant().getId(),
                history.getTenant().getName(),
                history.getSentAt(),
                history.getRecipientCount(),
                history.getFailureCount(),
                history.getStatus(),
                history.getSubject(),
                history.getErrorMessage()
        );
    }
}
