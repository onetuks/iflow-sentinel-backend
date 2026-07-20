package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.component.OAuth2TokenProvider;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.exception.ConnectorException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OAuth2TokenProviderTest {

    private static final String TOKEN_URL = "https://tenant.example.com/oauth/token";
    private static final String BASIC_AUTH_HEADER = "Basic " + Base64.getEncoder()
            .encodeToString("client-id:client-secret".getBytes(StandardCharsets.UTF_8));

    private MockRestServiceServer mockServer;
    private OAuth2TokenProvider tokenProvider;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        tokenProvider = new OAuth2TokenProvider(builder.build());
        tenant = TenantTestFixtures.tenant(1L, "https://tenant.example.com/api/v1", TOKEN_URL);
    }

    @Test
    void fetchTokenParsesAccessTokenFromResponse() {
        mockServer.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER))
                .andRespond(withSuccess("{\"access_token\":\"tok-123\",\"expires_in\":3600,\"token_type\":\"bearer\"}",
                        MediaType.APPLICATION_JSON));

        OAuth2TokenProvider.CachedToken token = tokenProvider.fetchToken(tenant);

        assertThat(token.accessToken()).isEqualTo("tok-123");
        mockServer.verify();
    }

    @Test
    void getAccessTokenCachesAndDoesNotRefetchWithinExpiry() {
        mockServer.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"tok-123\",\"expires_in\":3600,\"token_type\":\"bearer\"}",
                        MediaType.APPLICATION_JSON));

        String first = tokenProvider.getAccessToken(tenant);
        String second = tokenProvider.getAccessToken(tenant);

        assertThat(first).isEqualTo("tok-123");
        assertThat(second).isEqualTo("tok-123");
        mockServer.verify(); // 두 번째 호출이 서버를 다시 치면 "예상치 못한 요청"으로 실패한다
    }

    @Test
    void unauthorizedResponseThrowsConnectorExceptionWithStatusCode() {
        mockServer.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("invalid_client"));

        assertThatThrownBy(() -> tokenProvider.fetchToken(tenant))
                .isInstanceOf(ConnectorException.class)
                .satisfies(e -> assertThat(((ConnectorException) e).statusCode()).isEqualTo(401));
    }
}
