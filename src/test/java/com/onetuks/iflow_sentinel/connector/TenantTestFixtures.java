package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantAuthType;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantPlatform;

import org.springframework.test.util.ReflectionTestUtils;

/** DB 없이 순수 단위 테스트에서 Tenant 엔티티를 만들기 위한 헬퍼. id는 리플렉션으로 채운다(JPA 생성 값 대신). */
public final class TenantTestFixtures {

    private TenantTestFixtures() {
    }

    public static Tenant tenant(Long id, String odataUrl, String tokenUrl) {
        Project project = Project.builder().name("Test Project").build();
        Tenant tenant = Tenant.builder()
                .project(project)
                .name("Test Tenant")
                .odataUrl(odataUrl)
                .tokenUrl(tokenUrl)
                .platformType(TenantPlatform.CLOUD_FOUNDRY)
                .authType(TenantAuthType.OAUTH2_CLIENT_CREDENTIALS)
                .clientId("client-id")
                .clientSecret("client-secret")
                .interfaceUrl("https://test-rt.cfapps.eu10.hana.ondemand.com")
                .interfaceTokenUrl("https://test.authentication.eu10.hana.ondemand.com/oauth/token")
                .interfaceAuthType(TenantAuthType.BASIC)
                .interfaceUsername("iflow-user")
                .interfacePassword("iflow-password")
                .build();
        ReflectionTestUtils.setField(tenant, "id", id);
        return tenant;
    }
}
