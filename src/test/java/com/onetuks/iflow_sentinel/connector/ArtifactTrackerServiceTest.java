package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.connector.dto.ArtifactConfigurationResponse;
import com.onetuks.iflow_sentinel.connector.dto.ArtifactDeploymentStatus;
import com.onetuks.iflow_sentinel.connector.dto.SapArtifactDto;
import com.onetuks.iflow_sentinel.connector.dto.SapConfigurationDto;
import com.onetuks.iflow_sentinel.connector.dto.SapPackageDto;
import com.onetuks.iflow_sentinel.connector.dto.SapRuntimeArtifactDto;
import com.onetuks.iflow_sentinel.connector.dto.TrackerArtifactResponse;
import com.onetuks.iflow_sentinel.connector.service.ArtifactTrackerService;
import com.onetuks.iflow_sentinel.connector.service.PackageDefaultValueService;
import com.onetuks.iflow_sentinel.exception.ConnectorException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
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

    @Mock
    private ArtifactRepository artifactRepository;

    @Mock
    private PackageDefaultValueService packageDefaultValueService;

    private final Tenant tenant = TenantTestFixtures.tenant(1L, "https://tenant.example.com/api/v1",
            "https://tenant.example.com/oauth/token");

    @Test
    void mergesDesigntimeAndRuntimeArtifactsIntoDeploymentStatuses() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(odataClient.getCollection(eq(tenant), eq("/IntegrationPackages"), any()))
                .thenReturn(List.of(new SapPackageDto("PKG1", "Package One", "EDIT_ALLOWED")));
        when(odataClient.getCollection(eq(tenant),
                eq("/IntegrationPackages('PKG1')/IntegrationDesigntimeArtifacts"), any()))
                .thenReturn(List.of(
                        new SapArtifactDto("ART1", "Art One", "1.0.0", "PKG1"),
                        new SapArtifactDto("ART2", "Art Two", "1.0.0", "PKG1")));
        when(odataClient.getCollection(eq(tenant), eq("/IntegrationRuntimeArtifacts"), any()))
                .thenReturn(List.of(
                        new SapRuntimeArtifactDto("ART1", "Art One", "1.0.0", "IFlow", "STARTED"),
                        new SapRuntimeArtifactDto("ART3", "Art Three", "1.0.0", "IFlow", "STARTED")));

        ArtifactTrackerService service = new ArtifactTrackerService(tenantRepository, odataClient, artifactRepository, packageDefaultValueService);
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

        ArtifactTrackerService service = new ArtifactTrackerService(tenantRepository, odataClient, artifactRepository, packageDefaultValueService);

        assertThatThrownBy(() -> service.list(99L)).isInstanceOf(NoSuchElementException.class);
    }

    private static final String CONFIGURATIONS_PATH = "/IntegrationDesigntimeArtifacts(Id='ART1',Version='active')/Configurations";

    @Test
    void returnsBothDefaultAndConfiguredValuesWhenTheyDiffer() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(odataClient.getCollection(eq(tenant), eq(CONFIGURATIONS_PATH), any()))
                .thenReturn(List.of(new SapConfigurationDto("Receiver_URL", "https://configured.example.com", "xsd:string")));
        when(packageDefaultValueService.getDefaultValues(eq(tenant), any(), eq("ART1"), eq("active")))
                .thenReturn(Map.of("Receiver_URL", "https://default.example.com"));

        ArtifactTrackerService service = new ArtifactTrackerService(tenantRepository, odataClient, artifactRepository, packageDefaultValueService);
        List<ArtifactConfigurationResponse> result = service.getConfigurations(1L, "ART1", "active");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Receiver_URL");
        assertThat(result.get(0).defaultValue()).isEqualTo("https://default.example.com");
        assertThat(result.get(0).configuredValue()).isEqualTo("https://configured.example.com");
        assertThat(result.get(0).dataType()).isEqualTo("xsd:string");
    }

    @Test
    void fallsBackToActiveVersionForConfigurationsAndResolvesPackageFromThatVersion() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        String reqPath = "/IntegrationDesigntimeArtifacts(Id='ART1',Version='2.0.0')/Configurations";

        when(odataClient.getCollection(eq(tenant), eq(reqPath), any()))
                .thenThrow(new ConnectorException("찾을 수 없습니다", 404));
        when(odataClient.getCollection(eq(tenant), eq(CONFIGURATIONS_PATH), any()))
                .thenReturn(List.of(new SapConfigurationDto("Receiver_URL", "https://configured.example.com", "xsd:string")));
        when(packageDefaultValueService.getDefaultValues(eq(tenant), any(), eq("ART1"), eq("active")))
                .thenReturn(Map.of("Receiver_URL", "https://default.example.com"));

        ArtifactTrackerService service = new ArtifactTrackerService(tenantRepository, odataClient, artifactRepository, packageDefaultValueService);
        List<ArtifactConfigurationResponse> result = service.getConfigurations(1L, "ART1", "2.0.0");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).defaultValue()).isEqualTo("https://default.example.com");
        assertThat(result.get(0).configuredValue()).isEqualTo("https://configured.example.com");
    }

    @Test
    void packageIdResolutionFailureFallsBackToDashDefaultValueButKeepsConfiguredValue() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(odataClient.getCollection(eq(tenant), eq(CONFIGURATIONS_PATH), any()))
                .thenReturn(List.of(new SapConfigurationDto("Receiver_URL", "https://configured.example.com", "xsd:string")));
        when(packageDefaultValueService.getDefaultValues(eq(tenant), any(), eq("ART1"), eq("active")))
                .thenReturn(Map.of());

        ArtifactTrackerService service = new ArtifactTrackerService(tenantRepository, odataClient, artifactRepository, packageDefaultValueService);
        List<ArtifactConfigurationResponse> result = service.getConfigurations(1L, "ART1", "active");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).defaultValue()).isEqualTo("-");
        assertThat(result.get(0).configuredValue()).isEqualTo("https://configured.example.com");
    }

    @Test
    void parameterPresentOnlyInConfigurationsMapsDefaultValueToDash() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(odataClient.getCollection(eq(tenant), eq(CONFIGURATIONS_PATH), any()))
                .thenReturn(List.of(
                        new SapConfigurationDto("Known_Param", "configured-known", "xsd:string"),
                        new SapConfigurationDto("Unknown_Param", "configured-unknown", "xsd:string")));
        when(packageDefaultValueService.getDefaultValues(eq(tenant), any(), eq("ART1"), eq("active")))
                .thenReturn(Map.of("Known_Param", "default-known"));

        ArtifactTrackerService service = new ArtifactTrackerService(tenantRepository, odataClient, artifactRepository, packageDefaultValueService);
        List<ArtifactConfigurationResponse> result = service.getConfigurations(1L, "ART1", "active");

        ArtifactConfigurationResponse known = result.stream().filter(r -> r.name().equals("Known_Param")).findFirst().orElseThrow();
        ArtifactConfigurationResponse unknown = result.stream().filter(r -> r.name().equals("Unknown_Param")).findFirst().orElseThrow();
        assertThat(known.defaultValue()).isEqualTo("default-known");
        assertThat(unknown.defaultValue()).isEqualTo("-");
    }

    @Test
    void nullConfiguredValueFromODataMapsToDash() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(odataClient.getCollection(eq(tenant), eq(CONFIGURATIONS_PATH), any()))
                .thenReturn(List.of(new SapConfigurationDto("Receiver_URL", null, "xsd:string")));
        when(packageDefaultValueService.getDefaultValues(eq(tenant), any(), eq("ART1"), eq("active")))
                .thenReturn(Map.of());

        ArtifactTrackerService service = new ArtifactTrackerService(tenantRepository, odataClient, artifactRepository, packageDefaultValueService);
        List<ArtifactConfigurationResponse> result = service.getConfigurations(1L, "ART1", "active");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).configuredValue()).isEqualTo("-");
    }

    private TrackerArtifactResponse findByArtifactId(List<TrackerArtifactResponse> list, String artifactId) {
        return list.stream().filter(a -> a.artifactId().equals(artifactId)).findFirst()
                .orElseThrow(() -> new AssertionError("아티팩트를 찾을 수 없습니다: " + artifactId));
    }
}

