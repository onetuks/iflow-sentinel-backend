package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantPlatform;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

/** DB 없이 순수 단위 테스트에서 Tenant 엔티티를 만들기 위한 헬퍼. id는 리플렉션으로 채운다(JPA 생성 값 대신). */
public final class TenantTestFixtures {

    private TenantTestFixtures() {
    }

    public static Tenant tenant(Long id, String apiUrl, String apiTokenUrl) {
        Project project = Project.builder().name("Test Project").build();
        Tenant tenant = Tenant.builder()
                .project(project)
                .name("Test Tenant")
                .platformType(TenantPlatform.CLOUD_FOUNDRY)
                .apiUrl(apiUrl)
                .apiTokenUrl(apiTokenUrl)
                .apiClientId("client-id")
                .apiClientSecret("client-secret")
                .apiCreateDate(LocalDate.now())
                .ifUrl("https://test-rt.cfapps.eu10.hana.ondemand.com")
                .ifTokenUrl("https://test.authentication.eu10.hana.ondemand.com/oauth/token")
                .ifClientID("iflow-user")
                .ifClientSecret("iflow-password")
                .ifCreateDate(LocalDate.now())
                .build();
        ReflectionTestUtils.setField(tenant, "id", id);
        return tenant;
    }
}
