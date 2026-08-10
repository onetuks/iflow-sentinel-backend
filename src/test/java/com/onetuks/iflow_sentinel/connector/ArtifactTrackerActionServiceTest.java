package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.connector.service.ArtifactTrackerActionService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtifactTrackerActionServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ArtifactRepository artifactRepository;

    @Mock
    private SapODataClient odataClient;

    private final Tenant tenant = TenantTestFixtures.tenant(1L, "https://tenant.example.com/api/v1",
            "https://tenant.example.com/oauth/token");

    @Test
    void deployCallsDeployFunctionImportWithActiveVersion() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        ArtifactTrackerActionService service = new ArtifactTrackerActionService(tenantRepository, artifactRepository,
                odataClient);
        service.deploy(1L, "ART1");

        verify(odataClient).executeAction(tenant, HttpMethod.POST,
                "/DeployIntegrationDesigntimeArtifact?Id='ART1'&Version='active'");
    }

    @Test
    void undeployCallsRuntimeArtifactDelete() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        ArtifactTrackerActionService service = new ArtifactTrackerActionService(tenantRepository, artifactRepository,
                odataClient);
        service.undeploy(1L, "ART1");

        verify(odataClient).executeAction(tenant, HttpMethod.DELETE, "/IntegrationRuntimeArtifacts('ART1')");
    }

    @Test
    void deleteDesigntimeArtifactCallsSapAndRemovesLocalRecordIfPresent() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        Artifact localArtifact = Artifact.builder().sapArtifactId("ART1").name("Art One").version("1.0.0").build();
        when(artifactRepository.findBySapArtifactId("ART1")).thenReturn(Optional.of(localArtifact));

        ArtifactTrackerActionService service = new ArtifactTrackerActionService(tenantRepository, artifactRepository,
                odataClient);
        service.deleteDesigntimeArtifact(1L, "ART1", "1.0.0");

        verify(odataClient).executeAction(tenant, HttpMethod.DELETE,
                "/IntegrationDesigntimeArtifacts(Id='ART1',Version='1.0.0')");
        verify(artifactRepository).delete(localArtifact);
    }

    @Test
    void deleteDesigntimeArtifactSkipsLocalDeletionWhenNoLocalRecord() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(artifactRepository.findBySapArtifactId("ART3")).thenReturn(Optional.empty());

        ArtifactTrackerActionService service = new ArtifactTrackerActionService(tenantRepository, artifactRepository,
                odataClient);
        service.deleteDesigntimeArtifact(1L, "ART3", "1.0.0");

        verify(artifactRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void throwsWhenTenantNotFound() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        ArtifactTrackerActionService service = new ArtifactTrackerActionService(tenantRepository, artifactRepository,
                odataClient);

        assertThatThrownBy(() -> service.deploy(99L, "ART1")).isInstanceOf(NoSuchElementException.class);
    }
}
