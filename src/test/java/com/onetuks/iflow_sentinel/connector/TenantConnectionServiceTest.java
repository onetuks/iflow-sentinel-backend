package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.component.OAuth2TokenProvider;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.dto.ConnectionTestResult;
import com.onetuks.iflow_sentinel.connector.service.TenantConnectionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TenantConnectionServiceTest {

    private static final String TOKEN_URL = "https://tenant.example.com/oauth/token";

    private MockRestServiceServer mockServer;
    private TenantConnectionService connectionService;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        connectionService = new TenantConnectionService(new OAuth2TokenProvider(builder.build()));
        tenant = TenantTestFixtures.tenant(1L, "https://tenant.example.com/api/v1", TOKEN_URL);
    }

    @Test
    void successfulTokenFetchYieldsSuccessResult() {
        mockServer.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"tok\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        ConnectionTestResult result = connectionService.testConnection(tenant);

        assertThat(result.success()).isTrue();
        assertThat(result.statusCode()).isEqualTo(200);
    }

    @Test
    void unauthorizedYieldsFailureResultWithStatusCode() {
        mockServer.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("invalid_client"));

        ConnectionTestResult result = connectionService.testConnection(tenant);

        assertThat(result.success()).isFalse();
        assertThat(result.statusCode()).isEqualTo(401);
    }
}
