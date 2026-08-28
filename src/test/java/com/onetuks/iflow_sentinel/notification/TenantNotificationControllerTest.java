package com.onetuks.iflow_sentinel.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onetuks.iflow_sentinel.notification.controller.TenantNotificationController;
import com.onetuks.iflow_sentinel.notification.domain.NotificationStatus;
import com.onetuks.iflow_sentinel.notification.dto.NotificationHistoryResponse;
import com.onetuks.iflow_sentinel.notification.dto.TenantNotificationConfigRequest;
import com.onetuks.iflow_sentinel.notification.dto.TenantNotificationConfigResponse;
import com.onetuks.iflow_sentinel.notification.dto.TestEmailRequest;
import com.onetuks.iflow_sentinel.notification.service.TenantFailureReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TenantNotificationControllerTest {

    @Mock
    private TenantFailureReportService reportService;

    @InjectMocks
    private TenantNotificationController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /api/tenants/{tenantId}/notifications - 설정 조회 성공")
    void getConfig_Success() throws Exception {
        TenantNotificationConfigResponse response = new TenantNotificationConfigResponse(
                1L, 1L, "TENANT_A", true, "user@test.com", 15, LocalDateTime.now(), LocalDateTime.now()
        );
        when(reportService.getConfig(1L)).thenReturn(response);

        mockMvc.perform(get("/api/tenants/1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantName").value("TENANT_A"))
                .andExpect(jsonPath("$.isEnabled").value(true))
                .andExpect(jsonPath("$.intervalMinutes").value(15))
                .andExpect(jsonPath("$.recipients").value("user@test.com"));
    }

    @Test
    @DisplayName("PUT /api/tenants/{tenantId}/notifications - 설정 갱신 성공")
    void updateConfig_Success() throws Exception {
        TenantNotificationConfigRequest request = new TenantNotificationConfigRequest(true, "new@test.com", 30);
        TenantNotificationConfigResponse response = new TenantNotificationConfigResponse(
                1L, 1L, "TENANT_A", true, "new@test.com", 30, LocalDateTime.now(), LocalDateTime.now()
        );
        when(reportService.updateConfig(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/tenants/1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipients").value("new@test.com"))
                .andExpect(jsonPath("$.intervalMinutes").value(30));
    }


    @Test
    @DisplayName("POST /api/tenants/{tenantId}/notifications/test-mail - 테스트 메일 발송 성공")
    void sendTestEmail_Success() throws Exception {
        TestEmailRequest request = new TestEmailRequest("dev@test.com");

        mockMvc.perform(post("/api/tenants/1/notifications/test-mail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(reportService).sendTestEmail(1L, "dev@test.com");
    }

    @Test
    @DisplayName("POST /api/tenants/{tenantId}/notifications/send-report - 리포트 즉시 발송 성공 (200 OK)")
    void sendReportNow_Success() throws Exception {
        NotificationHistoryResponse historyResponse = new NotificationHistoryResponse(
                1L, 1L, "TENANT_A", LocalDateTime.now(), 2, 5, NotificationStatus.SUCCESS,
                "[iFlow Sentinel] [경고] TENANT_A 테넌트 실패 메시지 알림 (5건)", null
        );
        when(reportService.reportFailuresForTenant(1L, true)).thenReturn(Optional.of(historyResponse));

        mockMvc.perform(post("/api/tenants/1/notifications/send-report?force=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failureCount").value(5))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /api/tenants/{tenantId}/notifications/send-report - 신규 실패 건 없음 (204 No Content)")
    void sendReportNow_NoContent() throws Exception {
        when(reportService.reportFailuresForTenant(1L, false)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/tenants/1/notifications/send-report"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/tenants/{tenantId}/notifications/histories - 발송 이력 목록 조회 성공")
    void getHistories_Success() throws Exception {
        NotificationHistoryResponse h1 = new NotificationHistoryResponse(
                1L, 1L, "TENANT_A", LocalDateTime.now(), 1, 3, NotificationStatus.SUCCESS,
                "Report 1", null
        );
        when(reportService.getHistories(1L)).thenReturn(List.of(h1));

        mockMvc.perform(get("/api/tenants/1/notifications/histories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failureCount").value(3));
    }
}
