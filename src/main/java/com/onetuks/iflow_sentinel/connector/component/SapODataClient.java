package com.onetuks.iflow_sentinel.connector.component;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.dto.ODataCollectionResponse;
import com.onetuks.iflow_sentinel.exception.ConnectorException;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

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
        try {
            ODataCollectionResponse<T> response = restClient.get()
                    .uri(tenant.getOdataUrl() + relativePath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(typeRef);
            if (response == null || response.d() == null || response.d().results() == null) {
                return List.of();
            }
            return response.d().results();
        } catch (RestClientResponseException e) {
            throw new ConnectorException("OData 호출 실패: HTTP " + e.getStatusCode().value() + " " + relativePath,
                    e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new ConnectorException("OData 엔드포인트에 연결할 수 없습니다: " + relativePath, -1, e);
        }
    }

    public byte[] getBinary(Tenant tenant, String relativePath) {
        String token = tokenProvider.getAccessToken(tenant);
        try {
            byte[] body = restClient.get()
                    .uri(tenant.getOdataUrl() + relativePath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(byte[].class);
            if (body == null) {
                throw new ConnectorException("빈 응답을 받았습니다: " + relativePath, 200);
            }
            return body;
        } catch (RestClientResponseException e) {
            throw new ConnectorException("아티팩트 다운로드 실패: HTTP " + e.getStatusCode().value() + " " + relativePath,
                    e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new ConnectorException("아티팩트 다운로드 중 연결할 수 없습니다: " + relativePath, -1, e);
        }
    }
}
