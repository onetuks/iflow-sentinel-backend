package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantAuthType;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantPlatform;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRepository;
import com.onetuks.iflow_sentinel.connector.dto.ConnectionTestResult;
import com.onetuks.iflow_sentinel.connector.dto.TenantRequest;
import com.onetuks.iflow_sentinel.connector.dto.TenantResponse;
import com.onetuks.iflow_sentinel.connector.service.TenantConnectionService;
import com.onetuks.iflow_sentinel.connector.service.TenantService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** TNT-003/004: 테넌트 생성·수정·삭제가 실제로 필드를 반영하고 토큰 검증 및 삭제를 위임하는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TenantConnectionService connectionService;

    @Mock
    private com.onetuks.iflow_sentinel.connector.service.PackageSyncService packageSyncService;

    private final Tenant tenant = TenantTestFixtures.tenant(1L, "https://old.example.com/api/v1",
            "https://old.example.com/oauth/token");

    @Test
    void createSucceedsWhenConnectionTestPasses() {
        TenantService service = new TenantService(tenantRepository, projectRepository, connectionService,
                packageSyncService);
        Project project = Project.builder().name("Test Project").build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(tenantRepository.existsByProjectIdAndOdataUrl(any(), any())).thenReturn(false);
        when(connectionService.testConnection(any())).thenReturn(new ConnectionTestResult(true, 200, "연결에 성공했습니다."));
        when(tenantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TenantRequest request = new TenantRequest(
                1L,
                "New Tenant",
                "https://new.example.com/api/v1",
                "https://new.example.com/oauth/token",
                TenantPlatform.CLOUD_FOUNDRY,
                TenantAuthType.OAUTH2_CLIENT_CREDENTIALS,
                "client-id",
                "client-secret");

        TenantResponse response = service.create(request);

        assertThat(response.name()).isEqualTo("New Tenant");
        verify(packageSyncService).syncPackages(any());
    }

    @Test
    void createThrowsExceptionWhenConnectionTestFails() {
        TenantService service = new TenantService(tenantRepository, projectRepository, connectionService,
                packageSyncService);
        Project project = Project.builder().name("Test Project").build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(tenantRepository.existsByProjectIdAndOdataUrl(any(), any())).thenReturn(false);
        when(connectionService.testConnection(any()))
                .thenReturn(new ConnectionTestResult(false, 401, "Unauthorized"));

        TenantRequest request = new TenantRequest(
                1L,
                "Invalid Tenant",
                "https://invalid.example.com/api/v1",
                "https://invalid.example.com/oauth/token",
                TenantPlatform.CLOUD_FOUNDRY,
                TenantAuthType.OAUTH2_CLIENT_CREDENTIALS,
                "invalid-id",
                "invalid-secret");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("테넌트 연결에 실패하였습니다");

        verify(tenantRepository, never()).save(any());
        verify(packageSyncService, never()).syncPackages(any());
    }

    @Test
    void updateAppliesAllRequestedFieldsToTheEntity() {
        TenantService service = new TenantService(tenantRepository, projectRepository, connectionService,
                packageSyncService);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
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
