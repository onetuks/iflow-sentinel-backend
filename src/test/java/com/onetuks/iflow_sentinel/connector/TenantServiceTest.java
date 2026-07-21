package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantAuthType;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantPlatform;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.connector.dto.TenantRequest;
import com.onetuks.iflow_sentinel.connector.dto.TenantResponse;
import com.onetuks.iflow_sentinel.connector.service.TenantConnectionService;
import com.onetuks.iflow_sentinel.connector.service.TenantService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** TNT-003/004: 테넌트 수정·삭제가 실제로 필드를 반영하고 삭제를 위임하는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private com.onetuks.iflow_sentinel.connector.domain.project.ProjectRepository projectRepository;

    @Mock
    private TenantConnectionService connectionService;

    @Mock
    private com.onetuks.iflow_sentinel.connector.service.PackageSyncService packageSyncService;

    private final Tenant tenant = TenantTestFixtures.tenant(1L, "https://old.example.com/api/v1",
            "https://old.example.com/oauth/token");

    @Test
    void updateAppliesAllRequestedFieldsToTheEntity() {
        TenantService service = new TenantService(tenantRepository, projectRepository, connectionService,
                packageSyncService);
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(tenant));
        when(tenantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TenantRequest request = new TenantRequest(
                null,
                "New Name",
                "https://new.example.com/api/v1",
                "https://new.example.com/oauth/token",
                TenantPlatform.NEO,
                TenantAuthType.OAUTH2_CLIENT_CREDENTIALS,
                "new-client-id",
                "new-client-secret");

        TenantResponse response = service.update(1L, request);

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.odataUrl()).isEqualTo("https://new.example.com/api/v1");
        assertThat(response.tokenUrl()).isEqualTo("https://new.example.com/oauth/token");
        assertThat(response.platformType()).isEqualTo(TenantPlatform.NEO);
        assertThat(response.clientId()).isEqualTo("new-client-id");
    }

    @Test
    void deleteDelegatesToRepository() {
        TenantService service = new TenantService(tenantRepository, projectRepository, connectionService,
                packageSyncService);

        service.delete(1L);

        verify(tenantRepository).deleteById(1L);
    }
}
