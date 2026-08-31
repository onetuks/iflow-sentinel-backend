package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.domain.integrationpackage.IntegrationPackage;
import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.connector.dto.ConnectionTestResult;
import com.onetuks.iflow_sentinel.connector.dto.TenantRequest;
import com.onetuks.iflow_sentinel.connector.dto.TenantResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

@Service
public class TenantService {

    /** SAP가 HTTP/2 커넥션당 허용하는 동시 스트림 수를 넘지 않도록 실제 동시 SAP 호출 수를 제한한다. */
    private static final int MAX_CONCURRENT_SAP_CALLS = 10;

    private final TenantRepository tenantRepository;
    private final ProjectRepository projectRepository;
    private final TenantConnectionService connectionService;
    private final PackageSyncService packageSyncService;
    private final ArtifactSyncService artifactSyncService;

    public TenantService(TenantRepository tenantRepository, ProjectRepository projectRepository,
            TenantConnectionService connectionService, PackageSyncService packageSyncService,
            ArtifactSyncService artifactSyncService) {
        this.tenantRepository = tenantRepository;
        this.projectRepository = projectRepository;
        this.connectionService = connectionService;
        this.packageSyncService = packageSyncService;
        this.artifactSyncService = artifactSyncService;
    }

    @Transactional
    public TenantResponse create(TenantRequest request) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new NoSuchElementException("프로젝트를 찾을 수 없습니다: " + request.projectId()));

        if (tenantRepository.existsByProjectIdAndOdataUrl(request.projectId(), request.odataUrl())) {
            throw new IllegalArgumentException("동일한 프로젝트에 이미 등록된 OData URL입니다.");
        }

        Tenant tenant = Tenant.builder()
                .project(project)
                .name(request.name())
                .odataUrl(request.odataUrl())
                .tokenUrl(request.tokenUrl())
                .platformType(request.platformType())
                .authType(request.authType())
                .clientId(request.clientId())
                .clientSecret(request.clientSecret())
                .interfaceUrl(request.interfaceUrl())
                .interfaceTokenUrl(request.interfaceTokenUrl())
                .interfaceAuthType(request.interfaceAuthType())
                .interfaceUsername(request.interfaceUsername())
                .interfacePassword(request.interfacePassword())
                .build();

        ConnectionTestResult connectionResult = connectionService.testConnection(tenant);
        if (!connectionResult.success()) {
            throw new IllegalArgumentException("테넌트 연결에 실패하였습니다: " + connectionResult.message());
        }

        Tenant saved = tenantRepository.save(tenant);
        syncTenantData(saved);
        return TenantResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> list(Long projectId) {
        List<Tenant> tenants = projectId != null ? tenantRepository.findByProjectId(projectId)
                : tenantRepository.findAll();
        return tenants.stream().map(TenantResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TenantResponse get(Long id) {
        return TenantResponse.from(findTenant(id));
    }

    @Transactional(readOnly = true)
    public ConnectionTestResult testConnection(TenantRequest request) {
        Tenant tenant = Tenant.builder()
                .name(request.name())
                .odataUrl(request.odataUrl())
                .tokenUrl(request.tokenUrl())
                .platformType(request.platformType())
                .authType(request.authType())
                .clientId(request.clientId())
                .clientSecret(request.clientSecret())
                .interfaceUrl(request.interfaceUrl())
                .interfaceTokenUrl(request.interfaceTokenUrl())
                .interfaceAuthType(request.interfaceAuthType())
                .interfaceUsername(request.interfaceUsername())
                .interfacePassword(request.interfacePassword())
                .build();
        return connectionService.testConnection(tenant);
    }

    @Transactional(readOnly = true)
    public ConnectionTestResult testConnection(Long id) {
        return connectionService.testConnection(findTenant(id));
    }

    public ConnectionTestResult testConnection(Long id, TenantRequest request) {
        Tenant existing = findTenant(id);
        String secret = (request.clientSecret() != null && !request.clientSecret().isBlank())
                ? request.clientSecret()
                : existing.getClientSecret();
        String interfacePass = (request.interfacePassword() != null && !request.interfacePassword().isBlank())
                ? request.interfacePassword()
                : existing.getInterfacePassword();
        Tenant tenant = Tenant.builder()
                .name(request.name())
                .odataUrl(request.odataUrl())
                .tokenUrl(request.tokenUrl())
                .platformType(request.platformType())
                .authType(request.authType())
                .clientId(request.clientId())
                .clientSecret(secret)
                .interfaceUrl(request.interfaceUrl() != null ? request.interfaceUrl() : existing.getInterfaceUrl())
                .interfaceTokenUrl(request.interfaceTokenUrl() != null ? request.interfaceTokenUrl() : existing.getInterfaceTokenUrl())
                .interfaceAuthType(request.interfaceAuthType() != null ? request.interfaceAuthType() : existing.getInterfaceAuthType())
                .interfaceUsername(request.interfaceUsername() != null ? request.interfaceUsername() : existing.getInterfaceUsername())
                .interfacePassword(interfacePass)
                .build();
        return connectionService.testConnection(tenant);
    }

    @Transactional
    public TenantResponse update(Long id, TenantRequest request) {
        Tenant tenant = findTenant(id);

        if (!tenant.getOdataUrl().equals(request.odataUrl()) &&
                tenantRepository.existsByProjectIdAndOdataUrl(tenant.getProject().getId(), request.odataUrl())) {
            throw new IllegalArgumentException("동일한 프로젝트에 이미 등록된 OData URL입니다.");
        }

        String newSecret = (request.clientSecret() != null && !request.clientSecret().isBlank())
                ? request.clientSecret()
                : tenant.getClientSecret();

        String newInterfacePassword = (request.interfacePassword() != null && !request.interfacePassword().isBlank())
                ? request.interfacePassword()
                : tenant.getInterfacePassword();

        tenant.update(
                request.name(),
                request.odataUrl(),
                request.tokenUrl(),
                request.platformType(),
                request.authType(),
                request.clientId(),
                newSecret,
                request.interfaceUrl() != null ? request.interfaceUrl() : tenant.getInterfaceUrl(),
                request.interfaceTokenUrl() != null ? request.interfaceTokenUrl() : tenant.getInterfaceTokenUrl(),
                request.interfaceAuthType() != null ? request.interfaceAuthType() : tenant.getInterfaceAuthType(),
                request.interfaceUsername(),
                newInterfacePassword);
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    @Transactional
    public void delete(Long id) {
        tenantRepository.deleteById(id);
    }

    @Transactional
    public void sync(Long id) {
        Tenant tenant = findTenant(id);
        syncTenantData(tenant);
    }

    private void syncTenantData(Tenant tenant) {
        List<IntegrationPackage> packages = packageSyncService.syncPackages(tenant);

        // 아티팩트 동기화는 편집 가능(EDIT_ALLOWED)한 패키지만 대상으로 한다.
        // READ_ONLY 패키지는 SAP 표준 제공 콘텐츠 등으로 고객이 직접 관리하지 않으므로 추적 대상에서 제외한다.
        List<IntegrationPackage> editablePackages = packages.stream()
                .filter(IntegrationPackage::isEditable)
                .toList();

        // 패키지별 아티팩트 동기화는 서로 독립적인 블로킹 SAP 호출이므로 가상 스레드로 동시 실행해 지연 시간을 줄인다.
        // syncArtifacts()는 내부적으로 실패를 잡아 skip하므로 여기서 별도 예외 처리는 필요 없다.
        Set<String> activeArtifactIds = ConcurrentHashMap.newKeySet();
        Semaphore concurrencyLimiter = new Semaphore(MAX_CONCURRENT_SAP_CALLS);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (IntegrationPackage pkg : editablePackages) {
                executor.submit(() -> {
                    try {
                        concurrencyLimiter.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    try {
                        artifactSyncService.syncArtifacts(pkg).forEach(
                                artifact -> activeArtifactIds.add(artifact.getSapArtifactId()));
                    } finally {
                        concurrencyLimiter.release();
                    }
                });
            }
        }

        artifactSyncService.cleanOrphanArtifacts(tenant, activeArtifactIds);

        Set<String> activePackageIds = packages.stream()
                .map(IntegrationPackage::getSapPackageId)
                .collect(Collectors.toSet());
        packageSyncService.cleanOrphanPackages(tenant, activePackageIds);
    }

    @Transactional(readOnly = true)
    private Tenant findTenant(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("테넌트를 찾을 수 없습니다: " + id));
    }
}
