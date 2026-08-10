package com.onetuks.iflow_sentinel.connector.component;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.dto.ODataCollectionResponse;
import com.onetuks.iflow_sentinel.exception.ConnectorException;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SAP IS OData API 인증 호출 공용 헬퍼. {@code tenant.odataUrl()}은 "/api/v1"까지 포함한 베이스
 * URL로 가정한다
 * (설계서 10장 엔드포인트와 일치). 응답은 OData V2 {"d":{"results":[...]}} 형식으로 가정하며,
 * 실제 테넌트로 검증 시 이 가정이 다르면 이 클래스만 수정하면 되도록 격리해 둔다.
 */
@Component
public class SapODataClient {

    private final OAuth2TokenProvider tokenProvider;
    private final RestClient restClient;

    public SapODataClient(OAuth2TokenProvider tokenProvider, RestClient restClient) {
        this.tokenProvider = tokenProvider;
        this.restClient = restClient;
    }

    public <T> List<T> getCollection(Tenant tenant, String relativePath,
            ParameterizedTypeReference<ODataCollectionResponse<T>> typeRef) {
        String token = tokenProvider.getAccessToken(tenant);
        String fullUrl = buildUrl(tenant.getOdataUrl(), relativePath);
        try {
            ODataCollectionResponse<T> response = restClient.get()
                    .uri(fullUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(typeRef);
            if (response == null || response.d() == null || response.d().results() == null) {
                return List.of();
            }
            return response.d().results();
        } catch (RestClientResponseException e) {
            throw new ConnectorException("OData 호출 실패 (HTTP " + e.getStatusCode().value() + "): " + fullUrl,
                    e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new ConnectorException("OData 엔드포인트에 연결할 수 없습니다: " + fullUrl + " (원인: " + e.getMessage() + ")", -1, e);
        }
    }

    public byte[] getBinary(Tenant tenant, String relativePath) {
        String token = tokenProvider.getAccessToken(tenant);
        String fullUrl = buildUrl(tenant.getOdataUrl(), relativePath);
        try {
            byte[] body = restClient.get()
                    .uri(fullUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(byte[].class);
            if (body == null) {
                throw new ConnectorException("빈 응답을 받았습니다: " + fullUrl, 200);
            }
            return body;
        } catch (RestClientResponseException e) {
            throw new ConnectorException("아티팩트 다운로드 실패 (HTTP " + e.getStatusCode().value() + "): " + fullUrl,
                    e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new ConnectorException("아티팩트 다운로드 중 연결할 수 없습니다: " + fullUrl + " (원인: " + e.getMessage() + ")", -1, e);
        }
    }

    /**
     * SAP OData 상태 변경 요청(POST/DELETE)을 실행한다. SAP OData는 GET이 아닌 요청에 대해 CSRF 토큰을
     * 요구하므로, 먼저 {@code X-CSRF-Token: Fetch} 헤더로 토큰/세션 쿠키를 받아온 뒤 실제 요청에 실어 보낸다.
     */
    public void executeAction(Tenant tenant, HttpMethod method, String relativePath) {
        String token = tokenProvider.getAccessToken(tenant);
        CsrfToken csrf = fetchCsrfToken(tenant, token);
        String fullUrl = buildUrl(tenant.getOdataUrl(), relativePath);
        try {
            restClient.method(method)
                    .uri(fullUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("X-CSRF-Token", csrf.token())
                    .header(HttpHeaders.COOKIE, csrf.cookie())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new ConnectorException("SAP 액션 호출 실패 (HTTP " + e.getStatusCode().value() + "): " + fullUrl,
                    e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new ConnectorException("SAP 엔드포인트에 연결할 수 없습니다: " + fullUrl + " (원인: " + e.getMessage() + ")", -1, e);
        }
    }

    private CsrfToken fetchCsrfToken(Tenant tenant, String accessToken) {
        String baseUrl = buildUrl(tenant.getOdataUrl(), "");
        try {
            ResponseEntity<Void> response = restClient.get()
                    .uri(baseUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("X-CSRF-Token", "Fetch")
                    .retrieve()
                    .toBodilessEntity();

            String token = response.getHeaders().getFirst("X-CSRF-Token");
            if (token == null) {
                throw new ConnectorException("CSRF 토큰을 발급받지 못했습니다.", 200);
            }
            List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            String cookie = setCookies == null ? "" : setCookies.stream()
                    .map(c -> c.split(";", 2)[0])
                    .collect(Collectors.joining("; "));
            return new CsrfToken(token, cookie);
        } catch (RestClientResponseException e) {
            throw new ConnectorException("CSRF 토큰 발급 실패 (HTTP " + e.getStatusCode().value() + "): " + baseUrl,
                    e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new ConnectorException("CSRF 토큰 발급 중 연결할 수 없습니다: " + baseUrl + " (원인: " + e.getMessage() + ")", -1, e);
        }
    }

    private String buildUrl(String baseUrl, String relativePath) {
        if (baseUrl == null) {
            baseUrl = "";
        }
        baseUrl = baseUrl.trim();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        if (!baseUrl.contains("/api/v1")) {
            baseUrl = baseUrl + "/api/v1";
        }

        if (relativePath == null || relativePath.isEmpty()) {
            return baseUrl;
        }

        if (!relativePath.startsWith("/")) {
            relativePath = "/" + relativePath;
        }

        return baseUrl + relativePath;
    }

    private record CsrfToken(String token, String cookie) {
    }
}
