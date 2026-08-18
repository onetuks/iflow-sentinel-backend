package com.onetuks.iflow_sentinel.connector.component;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.dto.ODataCollectionResponse;
import com.onetuks.iflow_sentinel.connector.dto.ODataEntityResponse;
import com.onetuks.iflow_sentinel.exception.ConnectorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(SapODataClient.class);

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
        log.info("[OUTBOUND SAP OData] GET {}", fullUrl);
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

    public <T> T getEntity(Tenant tenant, String relativePath, ParameterizedTypeReference<ODataEntityResponse<T>> typeRef) {
        String token = tokenProvider.getAccessToken(tenant);
        String fullUrl = buildUrl(tenant.getOdataUrl(), relativePath);
        log.info("[OUTBOUND SAP OData] GET (Entity) {}", fullUrl);
        try {
            ODataEntityResponse<T> response = restClient.get()
                    .uri(fullUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(typeRef);
            return response == null ? null : response.d();
        } catch (RestClientResponseException e) {
            throw new ConnectorException("OData 단건 조회 실패 (HTTP " + e.getStatusCode().value() + "): " + fullUrl,
                    e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new ConnectorException("OData 엔드포인트에 연결할 수 없습니다: " + fullUrl + " (원인: " + e.getMessage() + ")", -1, e);
        }
    }

    public byte[] getBinary(Tenant tenant, String relativePath) {
        String token = tokenProvider.getAccessToken(tenant);
        String fullUrl = buildUrl(tenant.getOdataUrl(), relativePath);
        log.info("[OUTBOUND SAP OData] GET (Binary) {}", fullUrl);
        try {
            byte[] body = restClient.get()
                    .uri(java.net.URI.create(fullUrl))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(byte[].class);
            if (body == null) {
                throw new ConnectorException("빈 응답을 받았습니다: " + fullUrl, 200);
            }
            return body;
        } catch (RestClientResponseException e) {
            throw new ConnectorException("OData 바이너리/Payload 다운로드 실패 (HTTP " + e.getStatusCode().value() + "): " + fullUrl,
                    e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new ConnectorException("OData 엔드포인트에 연결할 수 없습니다: " + fullUrl + " (원인: " + e.getMessage() + ")", -1, e);
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
        log.info("[OUTBOUND SAP OData] {} {}", method, fullUrl);
        try {
            restClient.method(method)
                    .uri(java.net.URI.create(fullUrl))
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

    public List<com.onetuks.iflow_sentinel.reprocess.dto.SapMplLogDto> getMplFailures(Tenant tenant, String sapArtifactId, int top) {
        int fetchLimit = top > 0 ? Math.max(top * 3, 100) : 100;
        
        // 7일 전 타임스탬프 계산 (ISO-8601 OData V2 datetime 포맷)
        java.time.LocalDateTime oneWeekAgo = java.time.LocalDateTime.now().minusDays(7);
        String dateFilterStr = "LogStart ge datetime'" + oneWeekAgo.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + "'";

        // 1차 시도: 아티팩트 ID/Name + 7일 전 타임스탬프 필터 ($expand 사용 금지: 501 에러 방지)
        if (sapArtifactId != null && !sapArtifactId.isBlank()) {
            try {
                String path1 = "/MessageProcessingLogs?$filter=" + dateFilterStr + 
                               " and (IntegrationArtifact/Id eq '" + sapArtifactId + 
                               "' or IntegrationArtifact/Name eq '" + sapArtifactId + 
                               "' or IntegrationFlowName eq '" + sapArtifactId + "')" +
                               "&$top=" + fetchLimit + "&$orderby=LogStart desc";
                List<com.onetuks.iflow_sentinel.reprocess.dto.SapMplLogDto> logs = getCollection(
                        tenant, path1,
                        new ParameterizedTypeReference<ODataCollectionResponse<com.onetuks.iflow_sentinel.reprocess.dto.SapMplLogDto>>() {}
                );
                if (logs != null && !logs.isEmpty()) {
                    return logs;
                }
            } catch (Exception e1) {
                log.info("1차 OData 아티팩트 필터 쿼리 시도 실패 (사유: {}). 2차 전역 에러 쿼리 시도.", e1.getMessage());
            }
        }

        // 2차 시도: 최근 7일간 실패/에러 상태 전역 필터 쿼리 (Status eq 'FAILED' or 'ESCALATED' or 'CANCELLED')
        try {
            String path2 = "/MessageProcessingLogs?$filter=" + dateFilterStr + 
                           " and (Status eq 'FAILED' or Status eq 'ESCALATED' or Status eq 'CANCELLED')" +
                           "&$top=" + fetchLimit + "&$orderby=LogStart desc";
            List<com.onetuks.iflow_sentinel.reprocess.dto.SapMplLogDto> logs = getCollection(
                    tenant, path2,
                    new ParameterizedTypeReference<ODataCollectionResponse<com.onetuks.iflow_sentinel.reprocess.dto.SapMplLogDto>>() {}
            );
            if (logs != null && !logs.isEmpty()) {
                return logs;
            }
        } catch (Exception e2) {
            log.info("2차 Status 필터 쿼리 시도 실패 (사유: {}). 3차 7일 전 최신 로그 쿼리 시도.", e2.getMessage());
        }

        // 3차 시도: 7일 전 최신 로그 수집 (Status 필터 오작동 시 안전선)
        try {
            String path3 = "/MessageProcessingLogs?$filter=" + dateFilterStr + "&$top=" + fetchLimit + "&$orderby=LogStart desc";
            return getCollection(
                    tenant, path3,
                    new ParameterizedTypeReference<ODataCollectionResponse<com.onetuks.iflow_sentinel.reprocess.dto.SapMplLogDto>>() {}
            );
        } catch (Exception e3) {
            log.warn("3차 OData 최신 로그 수집 쿼리 시도 실패: {}", e3.getMessage());
            return List.of();
        }
    }

    /**
     * 특정 MessageGuid에 대한 평문 에러 상세 메시지 조회 API
     * GET /api/v1/MessageProcessingLogErrorInformations('{messageGuid}')/$value
     */
    public String getMplLogErrorInformation(Tenant tenant, String messageGuid) {
        if (messageGuid == null || messageGuid.isBlank()) {
            return null;
        }
        try {
            String relativePath = "/MessageProcessingLogErrorInformations('" + messageGuid + "')/$value";
            byte[] rawBytes = getBinary(tenant, relativePath);
            if (rawBytes != null && rawBytes.length > 0) {
                return new String(rawBytes, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.debug("MessageProcessingLogErrorInformations 평문 조회 실패 (MessageGuid={}): {}", messageGuid, e.getMessage());
        }
        return null;
    }

    private String buildUrl(String baseUrl, String relativePath) {
        if (baseUrl == null) {
            baseUrl = "";
        }
        baseUrl = baseUrl.trim();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        if (relativePath != null && !relativePath.startsWith("/api/") && !baseUrl.contains("/api/v1")) {
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
