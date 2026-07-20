package com.onetuks.iflow_sentinel.connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.onetuks.iflow_sentinel.domain.tenant.Tenant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth2 Client Credentials 방식으로 테넌트별 액세스 토큰을 발급·캐싱한다(설계서 3.3, BTP XSUAA 표준).
 * 테넌트당 하나의 토큰을 만료 임박 전까지 재사용하며, 만료 60초 전에는 안전 마진을 두고 갱신한다.
 */
@Component
public class OAuth2TokenProvider {

    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 60;

    private final RestClient restClient;
    private final ConcurrentHashMap<Long, CachedToken> cache = new ConcurrentHashMap<>();

    public OAuth2TokenProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    /** 캐시된 토큰이 있으면 재사용하고, 없거나 만료됐으면 새로 발급받는다. */
    public String getAccessToken(Tenant tenant) {
        CachedToken cached = cache.get(tenant.getId());
        if (cached != null && cached.isValid()) {
            return cached.accessToken();
        }
        CachedToken fresh = fetchToken(tenant);
        cache.put(tenant.getId(), fresh);
        return fresh.accessToken();
    }

    /** 캐시를 거치지 않고 항상 새로 토큰 발급을 시도한다. 연결 테스트(TNT-005) 전용. */
    public CachedToken fetchToken(Tenant tenant) {
        try {
            TokenResponse response = restClient.post()
                    .uri(tenant.getTokenUrl())
                    .header(HttpHeaders.AUTHORIZATION, basicAuth(tenant.getClientId(), tenant.getClientSecret()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new ConnectorException("토큰 응답에 access_token이 없습니다.", 200);
            }
            Instant expiresAt = Instant.now().plusSeconds(Math.max(response.expiresIn() - EXPIRY_SAFETY_MARGIN_SECONDS, 0));
            return new CachedToken(response.accessToken(), expiresAt);
        } catch (RestClientResponseException e) {
            throw new ConnectorException("토큰 발급 실패: HTTP " + e.getStatusCode().value(), e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new ConnectorException("토큰 엔드포인트에 연결할 수 없습니다: " + e.getMessage(), -1, e);
        }
    }

    private static String basicAuth(String clientId, String clientSecret) {
        String credentials = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    public record CachedToken(String accessToken, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("token_type") String tokenType
    ) {
    }
}
