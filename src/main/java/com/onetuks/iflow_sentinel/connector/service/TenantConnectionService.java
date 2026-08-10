package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.dto.ConnectionTestResult;
import com.onetuks.iflow_sentinel.connector.dto.ODataCollectionResponse;
import com.onetuks.iflow_sentinel.connector.dto.SapPackageDto;
import com.onetuks.iflow_sentinel.exception.ConnectorException;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

/** 테넌트 연결 테스트(TNT-005): 실제 OAuth2 토큰 발급 및 OData API 조회를 시도해 성립 여부를 판단한다. */
@Service
public class TenantConnectionService {

    private final SapODataClient odataClient;

    public TenantConnectionService(SapODataClient odataClient) {
        this.odataClient = odataClient;
    }

    public ConnectionTestResult testConnection(Tenant tenant) {
        try {
            odataClient.getCollection(tenant, "/IntegrationPackages",
                    new ParameterizedTypeReference<ODataCollectionResponse<SapPackageDto>>() {
                    });
            return new ConnectionTestResult(true, 200, "연결에 성공했습니다.");
        } catch (ConnectorException e) {
            return new ConnectionTestResult(false, e.statusCode(), e.getMessage());
        }
    }
}
