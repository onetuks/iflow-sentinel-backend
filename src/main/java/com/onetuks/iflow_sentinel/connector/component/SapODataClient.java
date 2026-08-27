package com.onetuks.iflow_sentinel.connector.component;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantAuthType;
import com.onetuks.iflow_sentinel.connector.dto.ODataCollectionResponse;
import com.onetuks.iflow_sentinel.connector.dto.ODataEntityResponse;
import com.onetuks.iflow_sentinel.connector.dto.SapMplLogLevelRequest;
import com.onetuks.iflow_sentinel.connector.dto.SapRuntimeArtifactDto;
import com.onetuks.iflow_sentinel.exception.ConnectorException;
import com.onetuks.iflow_sentinel.reprocess.dto.SapEntryPointDto;
import com.onetuks.iflow_sentinel.reprocess.dto.SapMplLogDto;
import com.onetuks.iflow_sentinel.reprocess.dto.SapServiceEndpointDto;
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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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
    private static final int MAX_PAGINATION_PAGES = 200;
    private static final String OPERATIONS_SET_MPL_LOG_LEVEL_PATH =
            "/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentSetMplLogLevelCommand";

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
            throw new ConnectorException("OData 엔드포인트에 연결할 수 없습니다: " + fullUrl + " (원인: " + e.getMessage() + ")", -1,
                    e);
        }
    }

    /**
     * SAP OData 컬렉션이 {@code __next}로 페이지네이션되어 있을 때, 페이지를 순회하며 predicate에
     * 일치하는 첫 엔트리를 찾는다. DataStoreEntries처럼 {@code $filter}가 지원되지 않는 프로퍼티로
     * 검색해야 할 때 사용한다. 일치 항목을 찾으면 즉시 반환하고 더 이상 페이지를 조회하지 않는다.
     */
    public <T> Optional<T> findInCollection(Tenant tenant, String relativePath,
            ParameterizedTypeReference<ODataCollectionResponse<T>> typeRef, Predicate<T> predicate) {
        String token = tokenProvider.getAccessToken(tenant);
        String url = buildUrl(tenant.getOdataUrl(), relativePath);
        int pagesFetched = 0;
        while (url != null && pagesFetched < MAX_PAGINATION_PAGES) {
            pagesFetched++;
            log.info("[OUTBOUND SAP OData] GET (Paginated, page {}) {}", pagesFetched, url);
            ODataCollectionResponse<T> response;
            try {
                response = restClient.get()
                        .uri(URI.create(url))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(typeRef);
            } catch (RestClientResponseException e) {
                log.error(
                        "[OUTBOUND SAP OData] GET (Paginated, page {}) 실패 - HTTP Status: {}, URL: {}, ResponseBody: {}",
                        pagesFetched, e.getStatusCode().value(), url, e.getResponseBodyAsString(), e);
                throw new ConnectorException("OData 호출 실패 (HTTP " + e.getStatusCode().value() + "): " + url,
                        e.getStatusCode().value(), e);
            } catch (ResourceAccessException e) {
                log.error("[OUTBOUND SAP OData] GET (Paginated, page {}) 연결 실패 - URL: {}, Message: {}",
                        pagesFetched, url, e.getMessage(), e);
                throw new ConnectorException("OData 엔드포인트에 연결할 수 없습니다: " + url + " (원인: " + e.getMessage() + ")", -1,
                        e);
            } catch (Exception e) {
                log.error("[OUTBOUND SAP OData] GET (Paginated, page {}) 예외 발생 - URL: {}, Message: {}",
                        pagesFetched, url, e.getMessage(), e);
                throw e;
            }
            if (response == null || response.d() == null) {
                log.warn("[OUTBOUND SAP OData] GET (Paginated, page {}) 응답 또는 d 데이터가 null입니다.", pagesFetched);
                return Optional.empty();
            }
            List<T> results = response.d().results() != null ? response.d().results() : List.of();
            log.info("[OUTBOUND SAP OData] GET (Paginated, page {}) 수신 건수: {}건", pagesFetched, results.size());
            Optional<T> match = results.stream().filter(predicate).findFirst();
            if (match.isPresent()) {
                log.info("[OUTBOUND SAP OData] GET (Paginated) 조건 매칭 성공 - 페이지 {}", pagesFetched);
                return match;
            }
            String next = response.d().next();
            url = (next == null || next.startsWith("http://") || next.startsWith("https://"))
                    ? next
                    : buildUrl(tenant.getOdataUrl(), next);
        }
        log.warn("[OUTBOUND SAP OData] GET (Paginated) 총 {}페이지 검색 완료 후 조건 일치 항목을 찾지 못함", pagesFetched);
        return Optional.empty();
    }

    public <T> T getEntity(Tenant tenant, String relativePath,
            ParameterizedTypeReference<ODataEntityResponse<T>> typeRef) {
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
            log.error("[OUTBOUND SAP OData] GET (Entity) 실패 - HTTP Status: {}, URL: {}, ResponseBody: {}",
                    e.getStatusCode().value(), fullUrl, e.getResponseBodyAsString(), e);
            throw new ConnectorException("OData 단건 조회 실패 (HTTP " + e.getStatusCode().value() + "): " + fullUrl,
                    e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            log.error("[OUTBOUND SAP OData] GET (Entity) 연결 실패 - URL: {}, Message: {}", fullUrl, e.getMessage(), e);
            throw new ConnectorException("OData 엔드포인트에 연결할 수 없습니다: " + fullUrl + " (원인: " + e.getMessage() + ")", -1,
                    e);
        } catch (Exception e) {
            log.error("[OUTBOUND SAP OData] GET (Entity) 예외 발생 - URL: {}, Message: {}", fullUrl, e.getMessage(), e);
            throw e;
        }
    }

    public byte[] getBinary(Tenant tenant, String relativePath) {
        String token = tokenProvider.getAccessToken(tenant);
        String fullUrl = buildUrl(tenant.getOdataUrl(), relativePath);
        log.info("[OUTBOUND SAP OData] GET (Binary) {}", fullUrl);
        try {
            byte[] body = restClient.get()
                    .uri(URI.create(fullUrl))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(byte[].class);
            if (body == null) {
                log.warn("[OUTBOUND SAP OData] GET (Binary) 빈 응답 수신 - URL: {}", fullUrl);
                throw new ConnectorException("빈 응답을 받았습니다: " + fullUrl, 200);
            }
            log.info("[OUTBOUND SAP OData] GET (Binary) 성공 - URL: {}, 수신 바이트: {} bytes", fullUrl, body.length);
            return body;
        } catch (RestClientResponseException e) {
            log.error("[OUTBOUND SAP OData] GET (Binary) 실패 - HTTP Status: {}, URL: {}, ResponseBody: {}",
                    e.getStatusCode().value(), fullUrl, e.getResponseBodyAsString(), e);
            throw new ConnectorException(
                    "OData 바이너리/Payload 다운로드 실패 (HTTP " + e.getStatusCode().value() + "): " + fullUrl,
                    e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            log.error("[OUTBOUND SAP OData] GET (Binary) 연결 실패 - URL: {}, Message: {}", fullUrl, e.getMessage(), e);
            throw new ConnectorException("OData 엔드포인트에 연결할 수 없습니다: " + fullUrl + " (원인: " + e.getMessage() + ")", -1,
                    e);
        } catch (Exception e) {
            log.error("[OUTBOUND SAP OData] GET (Binary) 예외 발생 - URL: {}, Message: {}", fullUrl, e.getMessage(), e);
            throw e;
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
                    .uri(URI.create(fullUrl))
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
            String cookie = setCookies == null ? ""
                    : setCookies.stream()
                            .map(c -> c.split(";", 2)[0])
                            .collect(Collectors.joining("; "));
            return new CsrfToken(token, cookie);
        } catch (RestClientResponseException e) {
            throw new ConnectorException("CSRF 토큰 발급 실패 (HTTP " + e.getStatusCode().value() + "): " + baseUrl,
                    e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new ConnectorException("CSRF 토큰 발급 중 연결할 수 없습니다: " + baseUrl + " (원인: " + e.getMessage() + ")", -1,
                    e);
        }
    }

    public List<SapMplLogDto> getMplFailures(Tenant tenant, String sapArtifactId, int top) {
        int fetchLimit = top > 0 ? Math.max(top * 3, 100) : 100;

        // 7일 전 타임스탬프 계산 (ISO-8601 OData V2 datetime 포맷)
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        String dateFilterStr = "LogStart ge datetime'"
                + oneWeekAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + "'";

        // 1차 시도: 아티팩트 ID/Name + 7일 전 타임스탬프 필터 ($expand 사용 금지: 501 에러 방지)
        if (sapArtifactId != null && !sapArtifactId.isBlank()) {
            try {
                String path1 = "/MessageProcessingLogs?$filter=" + dateFilterStr +
                        " and (IntegrationArtifact/Id eq '" + sapArtifactId +
                        "' or IntegrationArtifact/Name eq '" + sapArtifactId +
                        "' or IntegrationFlowName eq '" + sapArtifactId + "')" +
                        "&$top=" + fetchLimit + "&$orderby=LogStart desc";
                List<SapMplLogDto> logs = getCollection(
                        tenant, path1,
                        new ParameterizedTypeReference<ODataCollectionResponse<SapMplLogDto>>() {
                        });
                if (logs != null && !logs.isEmpty()) {
                    return logs;
                }
            } catch (Exception e1) {
                log.info("1차 OData 아티팩트 필터 쿼리 시도 실패 (사유: {}). 2차 전역 에러 쿼리 시도.", e1.getMessage());
            }
        }

        // 2차 시도: 최근 7일간 실패/에러 상태 전역 필터 쿼리 (Status eq 'FAILED' or 'ESCALATED' or
        // 'CANCELLED')
        try {
            String path2 = "/MessageProcessingLogs?$filter=" + dateFilterStr +
                    " and (Status eq 'FAILED' or Status eq 'ESCALATED' or Status eq 'CANCELLED')" +
                    "&$top=" + fetchLimit + "&$orderby=LogStart desc";
            List<SapMplLogDto> logs = getCollection(
                    tenant, path2,
                    new ParameterizedTypeReference<ODataCollectionResponse<SapMplLogDto>>() {
                    });
            if (logs != null && !logs.isEmpty()) {
                return logs;
            }
        } catch (Exception e2) {
            log.info("2차 Status 필터 쿼리 시도 실패 (사유: {}). 3차 7일 전 최신 로그 쿼리 시도.", e2.getMessage());
        }

        // 3차 시도: 7일 전 최신 로그 수집 (Status 필터 오작동 시 안전선)
        try {
            String path3 = "/MessageProcessingLogs?$filter=" + dateFilterStr + "&$top=" + fetchLimit
                    + "&$orderby=LogStart desc";
            return getCollection(
                    tenant, path3,
                    new ParameterizedTypeReference<ODataCollectionResponse<SapMplLogDto>>() {
                    });
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
                return new String(rawBytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.debug("MessageProcessingLogErrorInformations 평문 조회 실패 (MessageGuid={}): {}", messageGuid,
                    e.getMessage());
        }
        return null;
    }

    /**
     * 특정 아티팩트명(Name)에 해당하는 SAP CPI 배포된 ServiceEndpoints 목록 조회 API
     * GET /api/v1/ServiceEndpoints?$filter=Name eq '{name}'&$expand=EntryPoints
     */
    public List<SapServiceEndpointDto> getServiceEndpointsByName(Tenant tenant, String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        // 1차 시도: $filter + $expand=EntryPoints
        try {
            String path = "/ServiceEndpoints?$filter=Name eq '" + name.trim() + "'&$expand=EntryPoints";
            List<SapServiceEndpointDto> endpoints = getCollection(
                    tenant,
                    path,
                    new ParameterizedTypeReference<ODataCollectionResponse<SapServiceEndpointDto>>() {
                    });
            if (endpoints != null && !endpoints.isEmpty()) {
                return endpoints;
            }
        } catch (Exception e) {
            log.debug("ServiceEndpoints($filter + $expand) 조회 실패 (Name={}): {}. $filter 단독 쿼리 시도.", name,
                    e.getMessage());
        }

        // 2차 시도: $filter 단독
        try {
            String path = "/ServiceEndpoints?$filter=Name eq '" + name.trim() + "'";
            return getCollection(
                    tenant,
                    path,
                    new ParameterizedTypeReference<ODataCollectionResponse<SapServiceEndpointDto>>() {
                    });
        } catch (Exception e) {
            log.warn("ServiceEndpoints($filter) 조회 실패 (Name={}): {}", name, e.getMessage());
            return List.of();
        }
    }

    /**
     * 특정 ServiceEndpoint의 EntryPoints 목록 조회 API
     * GET /api/v1/ServiceEndpoints('{serviceEndpointId}')/EntryPoints
     */
    public List<SapEntryPointDto> getEntryPointsForServiceEndpoint(Tenant tenant, String serviceEndpointId) {
        if (serviceEndpointId == null || serviceEndpointId.isBlank()) {
            return List.of();
        }
        try {
            String encodedId = URLEncoder.encode(serviceEndpointId, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            String path = "/ServiceEndpoints('" + encodedId + "')/EntryPoints";
            return getCollection(
                    tenant,
                    path,
                    new ParameterizedTypeReference<ODataCollectionResponse<SapEntryPointDto>>() {
                    });
        } catch (Exception e) {
            log.debug("ServiceEndpoints('{Id}')/EntryPoints 조회 실패 (Id={}): {}", serviceEndpointId, e.getMessage());
            return List.of();
        }
    }

    /**
     * SAP CPI 배포된 ServiceEndpoints 목록 조회 API
     * GET /api/v1/ServiceEndpoints
     */
    public List<SapServiceEndpointDto> getServiceEndpoints(Tenant tenant) {
        try {
            List<SapServiceEndpointDto> endpoints = getCollection(
                    tenant,
                    "/ServiceEndpoints?$expand=EntryPoints",
                    new ParameterizedTypeReference<ODataCollectionResponse<SapServiceEndpointDto>>() {
                    });
            if (endpoints != null && !endpoints.isEmpty()) {
                return endpoints;
            }
        } catch (Exception e) {
            log.debug("SAP ServiceEndpoints($expand) 조회 실패: {}. 일반 조회 시도.", e.getMessage());
        }

        try {
            return getCollection(
                    tenant,
                    "/ServiceEndpoints",
                    new ParameterizedTypeReference<ODataCollectionResponse<SapServiceEndpointDto>>() {
                    });
        } catch (Exception e) {
            log.warn("SAP ServiceEndpoints 목록 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 특정 런타임 아티팩트의 배포된 ServiceEndpoints 목록 조회 API
     * GET /api/v1/IntegrationRuntimeArtifacts('{id}')/ServiceEndpoints
     */
    public List<SapServiceEndpointDto> getServiceEndpointsForRuntimeArtifact(Tenant tenant, String runtimeArtifactId) {
        try {
            List<SapServiceEndpointDto> endpoints = getCollection(
                    tenant,
                    "/IntegrationRuntimeArtifacts('" + runtimeArtifactId + "')/ServiceEndpoints?$expand=EntryPoints",
                    new ParameterizedTypeReference<ODataCollectionResponse<SapServiceEndpointDto>>() {
                    });
            if (endpoints != null && !endpoints.isEmpty()) {
                return endpoints;
            }
        } catch (Exception e) {
            log.debug("특정 Runtime Artifact({}) ServiceEndpoints($expand) 조회 실패: {}. 일반 조회 시도.", runtimeArtifactId,
                    e.getMessage());
        }

        try {
            return getCollection(
                    tenant,
                    "/IntegrationRuntimeArtifacts('" + runtimeArtifactId + "')/ServiceEndpoints",
                    new ParameterizedTypeReference<ODataCollectionResponse<SapServiceEndpointDto>>() {
                    });
        } catch (Exception e) {
            log.debug("특정 Runtime Artifact({}) ServiceEndpoints 목록 조회 실패: {}", runtimeArtifactId, e.getMessage());
            return List.of();
        }
    }

    /**
     * 배포된 런타임 아티팩트 목록 조회 API
     * GET /api/v1/IntegrationRuntimeArtifacts
     */
    public List<SapRuntimeArtifactDto> getRuntimeArtifacts(Tenant tenant) {
        try {
            return getCollection(
                    tenant,
                    "/IntegrationRuntimeArtifacts",
                    new ParameterizedTypeReference<ODataCollectionResponse<SapRuntimeArtifactDto>>() {
                    });
        } catch (Exception e) {
            log.warn("SAP IntegrationRuntimeArtifacts 목록 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * SAP WebUI 내부 Operations 커맨드 API로 특정 배포된 런타임 아티팩트의 MPL 로그 레벨을 설정한다.
     * 표준 OData(/api/v1) API가 아니며, OAuth2 Bearer 토큰만으로 인증한다(CSRF/쿠키 불필요).
     * POST https://&lt;host&gt;/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentSetMplLogLevelCommand
     */
    public boolean setMplLogLevel(Tenant tenant, String artifactSymbolicName, String logLevel) {
        String token = tokenProvider.getAccessToken(tenant);
        String fullUrl = stripApiV1(tenant.getOdataUrl()) + OPERATIONS_SET_MPL_LOG_LEVEL_PATH;
        SapMplLogLevelRequest payload = SapMplLogLevelRequest.of(artifactSymbolicName, logLevel);

        log.info("[OUTBOUND SAP Operations] POST {} (artifact={}, level={})", fullUrl, artifactSymbolicName,
                logLevel);
        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(URI.create(fullUrl))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toEntity(String.class);

            String body = response.getBody() != null ? response.getBody() : "";
            boolean success = response.getStatusCode().is2xxSuccessful()
                    && (body.contains("logConfiguration") || body.contains("true"));
            if (!success) {
                log.warn("[OUTBOUND SAP Operations] MPL 로그 레벨 설정 응답이 예상과 다름 - URL: {}, Status: {}, Body: {}",
                        fullUrl, response.getStatusCode(), body);
            }
            return success;
        } catch (RestClientResponseException e) {
            log.error("[OUTBOUND SAP Operations] MPL 로그 레벨 설정 실패 - HTTP Status: {}, URL: {}, ResponseBody: {}",
                    e.getStatusCode().value(), fullUrl, e.getResponseBodyAsString(), e);
            throw new ConnectorException("MPL 로그 레벨 설정 실패 (HTTP " + e.getStatusCode().value() + "): " + fullUrl,
                    e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            log.error("[OUTBOUND SAP Operations] 연결 실패 - URL: {}, Message: {}", fullUrl, e.getMessage(), e);
            throw new ConnectorException("Operations 엔드포인트에 연결할 수 없습니다: " + fullUrl + " (원인: " + e.getMessage() + ")",
                    -1, e);
        }
    }

    /** OData 베이스 URL(.../api/v1 포함 가능)에서 스킴+호스트만 남긴다(Operations 엔드포인트는 /api/v1 경로를 쓰지 않음). */
    private String stripApiV1(String odataUrl) {
        String baseUrl = odataUrl != null ? odataUrl.trim() : "";
        if (baseUrl.contains("/api/v1")) {
            baseUrl = baseUrl.substring(0, baseUrl.indexOf("/api/v1"));
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    /**
     * iFlow 인터페이스 엔드포인트로 메시지 페이로드를 직접 POST 전송한다.
     * 테넌트에 등록된 인터페이스 인증 정보(Basic Auth 등)를 우선 사용하고, 없으면 테넌트 OAuth2 Bearer 토큰을 사용한다.
     */
    public ResponseEntity<String> callInterfaceEndpoint(Tenant tenant, String targetUrl, String payload,
            String contentType) {
        String authHeader;
        if (tenant.getInterfaceAuthType() == TenantAuthType.BASIC
                || (tenant.getInterfaceUsername() != null && !tenant.getInterfaceUsername().isBlank())) {
            String username = tenant.getInterfaceUsername() != null ? tenant.getInterfaceUsername() : "";
            String password = tenant.getInterfacePassword() != null ? tenant.getInterfacePassword() : "";
            String authString = username + ":" + password;
            authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString(authString.getBytes(StandardCharsets.UTF_8));
        } else {
            String token = tokenProvider.getAccessToken(tenant);
            authHeader = "Bearer " + token;
        }

        String fullUrl = targetUrl;
        if (!fullUrl.startsWith("http://") && !fullUrl.startsWith("https://")) {
            String baseUrl = (tenant.getInterfaceUrl() != null && !tenant.getInterfaceUrl().isBlank())
                    ? tenant.getInterfaceUrl().trim()
                    : (tenant.getOdataUrl() != null ? tenant.getOdataUrl() : "");

            if (baseUrl.contains("/api/v1")) {
                baseUrl = baseUrl.replace("/api/v1", "");
            }
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            if (!fullUrl.startsWith("/")) {
                fullUrl = "/" + fullUrl;
            }
            fullUrl = baseUrl + fullUrl;
        }

        MediaType mediaType = MediaType.APPLICATION_JSON;
        if (contentType != null && !contentType.isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(contentType);
            } catch (Exception ignored) {
            }
        }

        log.info("[OUTBOUND INTERFACE CALL] POST {} (Auth: {}, Content-Type: {}, payloadLength: {})",
                fullUrl, authHeader.startsWith("Basic") ? "Basic" : "Bearer", mediaType,
                payload != null ? payload.length() : 0);

        try {
            return restClient.post()
                    .uri(URI.create(fullUrl))
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .contentType(mediaType)
                    .body(payload != null ? payload : "")
                    .retrieve()
                    .toEntity(String.class);
        } catch (RestClientResponseException e) {
            log.error("[OUTBOUND INTERFACE CALL] 실패 - HTTP Status: {}, URL: {}, ResponseBody: {}",
                    e.getStatusCode().value(), fullUrl, e.getResponseBodyAsString(), e);
            throw new ConnectorException(
                    "인터페이스 직접 호출 실패 (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString(),
                    e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            log.error("[OUTBOUND INTERFACE CALL] 연결 실패 - URL: {}, Message: {}", fullUrl, e.getMessage(), e);
            throw new ConnectorException("인터페이스 엔드포인트에 연결할 수 없습니다: " + fullUrl + " (원인: " + e.getMessage() + ")", -1,
                    e);
        } catch (Exception e) {
            log.error("[OUTBOUND INTERFACE CALL] 예외 발생 - URL: {}, Message: {}", fullUrl, e.getMessage(), e);
            throw e;
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
