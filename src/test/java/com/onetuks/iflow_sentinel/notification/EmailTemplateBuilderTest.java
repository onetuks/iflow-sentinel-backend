package com.onetuks.iflow_sentinel.notification;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantAuthType;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantPlatform;
import com.onetuks.iflow_sentinel.notification.service.EmailTemplateBuilder;
import com.onetuks.iflow_sentinel.reprocess.dto.MplFailureResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateBuilderTest {

    private EmailTemplateBuilder emailTemplateBuilder;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        emailTemplateBuilder = new EmailTemplateBuilder("http://localhost:3000");
        testTenant = Tenant.builder()
                .name("PROD_CF_TENANT")
                .odataUrl("https://example.com/odata")
                .tokenUrl("https://example.com/oauth/token")
                .platformType(TenantPlatform.CLOUD_FOUNDRY)
                .authType(TenantAuthType.OAUTH2_CLIENT_CREDENTIALS)
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();
    }

    @Test
    @DisplayName("실패 리포트 HTML 본문 생성 시 테넌트명, 실패 건수, 에러 메시지 등이 포함되어야 한다")
    void buildFailureReportHtml_Success() {
        // given
        MplFailureResponse failure1 = new MplFailureResponse(
                "MSG-1001",
                "CORR-001",
                "FAILED",
                "IF_SAP_TO_ERP",
                "SAP to ERP Flow",
                LocalDateTime.of(2026, 8, 27, 10, 0, 0),
                LocalDateTime.of(2026, 8, 27, 10, 0, 5),
                "ERROR_DS",
                "DATASTORE",
                "NORMAL",
                30,
                "HTTP 500 Internal Server Error from Target"
        );

        // when
        String html = emailTemplateBuilder.buildFailureReportHtml(testTenant, List.of(failure1), LocalDateTime.now());

        // then
        assertThat(html).isNotNull();
        assertThat(html).contains("PROD_CF_TENANT");
        assertThat(html).contains("1건");
        assertThat(html).contains("SAP to ERP Flow");
        assertThat(html).contains("MSG-1001");
        assertThat(html).contains("HTTP 500 Internal Server Error");
        assertThat(html).contains("FAILED");
        assertThat(html).contains("http://localhost:3000/reprocess");
    }

    @Test
    @DisplayName("테스트 이메일 HTML 본문 생성 검증")
    void buildTestEmailHtml_Success() {
        // when
        String html = emailTemplateBuilder.buildTestEmailHtml(testTenant, "admin@example.com");

        // then
        assertThat(html).isNotNull();
        assertThat(html).contains("PROD_CF_TENANT");
        assertThat(html).contains("admin@example.com");
        assertThat(html).contains("정상 동작 중");
    }
}
