package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.component.OAuth2TokenProvider;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.dto.ConnectionTestResult;
import com.onetuks.iflow_sentinel.exception.ConnectorException;

import org.springframework.stereotype.Service;

/** 테넌트 연결 테스트(TNT-005): 실제 OAuth2 토큰 발급을 시도해 2xx 여부로 성립 여부를 판단한다. */
@Service
public class TenantConnectionService {

    private final OAuth2TokenProvider tokenProvider;

    public TenantConnectionService(OAuth2TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public ConnectionTestResult testConnection(Tenant tenant) {
        try {
            tokenProvider.fetchToken(tenant);
            return new ConnectionTestResult(true, 200, "연결에 성공했습니다.");
        } catch (ConnectorException e) {
            return new ConnectionTestResult(false, e.statusCode(), e.getMessage());
        }
    }
}
