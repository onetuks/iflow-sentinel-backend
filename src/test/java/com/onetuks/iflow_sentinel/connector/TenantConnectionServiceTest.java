package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.dto.ConnectionTestResult;
import com.onetuks.iflow_sentinel.connector.service.TenantConnectionService;
import com.onetuks.iflow_sentinel.exception.ConnectorException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TenantConnectionServiceTest {

    @Mock
    private SapODataClient odataClient;

    private TenantConnectionService connectionService;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        connectionService = new TenantConnectionService(odataClient);
        tenant = TenantTestFixtures.tenant(1L, "https://tenant.example.com/api/v1", "https://tenant.example.com/oauth/token");
    }

    @Test
    void successfulTokenFetchYieldsSuccessResult() {
        ConnectionTestResult result = connectionService.testConnection(tenant);

        assertThat(result.success()).isTrue();
        assertThat(result.statusCode()).isEqualTo(200);
        verify(odataClient).getCollection(any(), any(), any());
    }

    @Test
    void unauthorizedYieldsFailureResultWithStatusCode() {
        doThrow(new ConnectorException("Unauthorized", 401)).when(odataClient).getCollection(any(), any(), any());

        ConnectionTestResult result = connectionService.testConnection(tenant);

        assertThat(result.success()).isFalse();
        assertThat(result.statusCode()).isEqualTo(401);
    }
}
