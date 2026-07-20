package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.dto.SapPackageDto;
import com.onetuks.iflow_sentinel.domain.integrationpackage.IntegrationPackage;
import com.onetuks.iflow_sentinel.domain.integrationpackage.IntegrationPackageRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PackageSyncServiceTest {

    @Mock
    private SapODataClient odataClient;

    @Mock
    private IntegrationPackageRepository packageRepository;

    private final Tenant tenant = TenantTestFixtures.tenant(1L, "https://tenant.example.com/api/v1", "https://tenant.example.com/oauth/token");

    @Test
    void createsNewPackageWhenNotExisting() {
        when(odataClient.getCollection(eq(tenant), eq("/IntegrationPackages"), any()))
                .thenReturn(List.of(new SapPackageDto("PKG1", "Package One")));
        when(packageRepository.findBySapPackageId("PKG1")).thenReturn(Optional.empty());
        when(packageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PackageSyncService service = new PackageSyncService(odataClient, packageRepository);
        List<IntegrationPackage> result = service.syncPackages(tenant);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSapPackageId()).isEqualTo("PKG1");
        assertThat(result.get(0).getName()).isEqualTo("Package One");
    }

    @Test
    void renamesExistingPackageOnResync() {
        IntegrationPackage existing = IntegrationPackage.builder().tenant(tenant).sapPackageId("PKG1").name("Old Name").build();
        when(odataClient.getCollection(eq(tenant), eq("/IntegrationPackages"), any()))
                .thenReturn(List.of(new SapPackageDto("PKG1", "New Name")));
        when(packageRepository.findBySapPackageId("PKG1")).thenReturn(Optional.of(existing));
        when(packageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PackageSyncService service = new PackageSyncService(odataClient, packageRepository);
        List<IntegrationPackage> result = service.syncPackages(tenant);

        assertThat(result.get(0).getName()).isEqualTo("New Name");
        verify(packageRepository).save(existing);
    }
}
