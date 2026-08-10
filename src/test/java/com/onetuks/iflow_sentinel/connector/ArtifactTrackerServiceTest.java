package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.connector.dto.ArtifactDeploymentStatus;
import com.onetuks.iflow_sentinel.connector.dto.SapArtifactDto;
import com.onetuks.iflow_sentinel.connector.dto.SapPackageDto;
import com.onetuks.iflow_sentinel.connector.dto.SapRuntimeArtifactDto;
import com.onetuks.iflow_sentinel.connector.dto.TrackerArtifactResponse;
import com.onetuks.iflow_sentinel.connector.service.ArtifactTrackerService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtifactTrackerServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private SapODataClient odataClient;

    private final Tenant tenant = TenantTestFixtures.tenant(1L, "https://tenant.example.com/api/v1",
            "https://tenant.example.com/oauth/token");

    @Test
    void mergesDesigntimeAndRuntimeArtifactsIntoDeploymentStatuses() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(odataClient.getCollection(eq(tenant), eq("/IntegrationPackages"), any()))
                .thenReturn(List.of(new SapPackageDto("PKG1", "Package One")));
        when(odataClient.getCollection(eq(tenant),
                eq("/IntegrationPackages('PKG1')/IntegrationDesigntimeArtifacts"), any()))
                .thenReturn(List.of(
                        new SapArtifactDto("ART1", "Art One", "1.0.0", "PKG1"),
                        new SapArtifactDto("ART2", "Art Two", "1.0.0", "PKG1")));
        when(odataClient.getCollection(eq(tenant), eq("/IntegrationRuntimeArtifacts"), any()))
                .thenReturn(List.of(
                        new SapRuntimeArtifactDto("ART1", "Art One", "1.0.0", "IFlow", "STARTED"),
                        new SapRuntimeArtifactDto("ART3", "Art Three", "1.0.0", "IFlow", "STARTED")));

        ArtifactTrackerService service = new ArtifactTrackerService(tenantRepository, odataClient);
        List<TrackerArtifactResponse> result = service.list(1L);

        assertThat(result).hasSize(3);

        TrackerArtifactResponse deployed = findByArtifactId(result, "ART1");
        assertThat(deployed.status()).isEqualTo(ArtifactDeploymentStatus.DEPLOYED);
        assertThat(deployed.packageId()).isEqualTo("PKG1");
        assertThat(deployed.runtimeStatus()).isEqualTo("STARTED");

        TrackerArtifactResponse notDeployed = findByArtifactId(result, "ART2");
        assertThat(notDeployed.status()).isEqualTo(ArtifactDeploymentStatus.NOT_DEPLOYED);
        assertThat(notDeployed.runtimeStatus()).isNull();

        TrackerArtifactResponse inactive = findByArtifactId(result, "ART3");
        assertThat(inactive.status()).isEqualTo(ArtifactDeploymentStatus.INACTIVE);
        assertThat(inactive.packageId()).isNull();
    }

    @Test
    void throwsWhenTenantNotFound() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        ArtifactTrackerService service = new ArtifactTrackerService(tenantRepository, odataClient);

        assertThatThrownBy(() -> service.list(99L)).isInstanceOf(NoSuchElementException.class);
    }

    private TrackerArtifactResponse findByArtifactId(List<TrackerArtifactResponse> list, String artifactId) {
        return list.stream().filter(a -> a.artifactId().equals(artifactId)).findFirst()
                .orElseThrow(() -> new AssertionError("아티팩트를 찾을 수 없습니다: " + artifactId));
    }
}
