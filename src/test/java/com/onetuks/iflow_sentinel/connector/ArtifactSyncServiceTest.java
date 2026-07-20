package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.dto.SapArtifactDto;
import com.onetuks.iflow_sentinel.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.domain.artifact.ArtifactType;
import com.onetuks.iflow_sentinel.domain.integrationpackage.IntegrationPackage;
import com.onetuks.iflow_sentinel.domain.tenant.Tenant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtifactSyncServiceTest {

    @Mock
    private SapODataClient odataClient;

    @Mock
    private ArtifactRepository artifactRepository;

    private final Tenant tenant = TenantTestFixtures.tenant(1L, "https://tenant.example.com/api/v1", "https://tenant.example.com/oauth/token");
    private final IntegrationPackage integrationPackage = IntegrationPackage.builder().tenant(tenant).sapPackageId("PKG1").name("Package One").build();

    @Test
    void createsNewArtifactAsIflowType() {
        when(odataClient.getCollection(eq(tenant), eq("/IntegrationPackages('PKG1')/IntegrationDesigntimeArtifacts"), any()))
                .thenReturn(List.of(new SapArtifactDto("ART1", "My Flow", "1.0.0", "PKG1")));
        when(artifactRepository.findBySapArtifactId("ART1")).thenReturn(Optional.empty());
        when(artifactRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ArtifactSyncService service = new ArtifactSyncService(odataClient, artifactRepository);
        List<Artifact> result = service.syncArtifacts(integrationPackage);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSapArtifactId()).isEqualTo("ART1");
        assertThat(result.get(0).getName()).isEqualTo("My Flow");
        assertThat(result.get(0).getVersion()).isEqualTo("1.0.0");
        assertThat(result.get(0).getType()).isEqualTo(ArtifactType.IFLOW);
    }
}
