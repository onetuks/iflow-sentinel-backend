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

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TenantService {

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

        if (tenantRepository.existsByProjectIdAndApiUrl(request.projectId(), request.apiUrl())) {
            throw new IllegalArgumentException("동일한 프로젝트에 이미 등록된 API URL입니다.");
        }

        Tenant tenant = Tenant.builder()
                .project(project)
                .name(request.name())
                .platformType(request.platformType())
                .apiUrl(request.apiUrl())
                .apiTokenUrl(request.apiTokenUrl())
                .apiClientId(request.apiClientId())
                .apiClientSecret(request.apiClientSecret())
                .apiCreateDate(request.apiCreateDate())
                .ifUrl(request.ifUrl())
                .ifTokenUrl(request.ifTokenUrl())
                .ifClientID(request.ifClientID())
                .ifClientSecret(request.ifClientSecret())
                .ifCreateDate(request.ifCreateDate())
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
                .platformType(request.platformType())
                .apiUrl(request.apiUrl())
                .apiTokenUrl(request.apiTokenUrl())
                .apiClientId(request.apiClientId())
                .apiClientSecret(request.apiClientSecret())
                .apiCreateDate(request.apiCreateDate())
                .ifUrl(request.ifUrl())
                .ifTokenUrl(request.ifTokenUrl())
                .ifClientID(request.ifClientID())
                .ifClientSecret(request.ifClientSecret())
                .ifCreateDate(request.ifCreateDate())
                .build();
        return connectionService.testConnection(tenant);
    }

    @Transactional(readOnly = true)
    public ConnectionTestResult testConnection(Long id) {
        return connectionService.testConnection(findTenant(id));
    }

    public ConnectionTestResult testConnection(Long id, TenantRequest request) {
        Tenant existing = findTenant(id);
        String secret = (request.apiClientSecret() != null && !request.apiClientSecret().isBlank())
                ? request.apiClientSecret()
                : existing.getApiClientSecret();
        String ifSecret = (request.ifClientSecret() != null && !request.ifClientSecret().isBlank())
                ? request.ifClientSecret()
                : existing.getIfClientSecret();
        Tenant tenant = Tenant.builder()
                .name(request.name())
                .platformType(request.platformType())
                .apiUrl(request.apiUrl())
                .apiTokenUrl(request.apiTokenUrl())
                .apiClientId(request.apiClientId())
                .apiClientSecret(secret)
                .apiCreateDate(request.apiCreateDate() != null ? request.apiCreateDate() : existing.getApiCreateDate())
                .ifUrl(request.ifUrl() != null ? request.ifUrl() : existing.getIfUrl())
                .ifTokenUrl(request.ifTokenUrl() != null ? request.ifTokenUrl() : existing.getIfTokenUrl())
                .ifClientID(request.ifClientID() != null ? request.ifClientID() : existing.getIfClientID())
                .ifClientSecret(ifSecret)
                .ifCreateDate(request.ifCreateDate() != null ? request.ifCreateDate() : existing.getIfCreateDate())
                .build();
        return connectionService.testConnection(tenant);
    }

    @Transactional
    public TenantResponse update(Long id, TenantRequest request) {
        Tenant tenant = findTenant(id);

        if (!tenant.getApiUrl().equals(request.apiUrl()) &&
                tenantRepository.existsByProjectIdAndApiUrl(tenant.getProject().getId(), request.apiUrl())) {
            throw new IllegalArgumentException("동일한 프로젝트에 이미 등록된 API URL입니다.");
        }

        String newSecret = (request.apiClientSecret() != null && !request.apiClientSecret().isBlank())
                ? request.apiClientSecret()
                : tenant.getApiClientSecret();

        String newIfSecret = (request.ifClientSecret() != null && !request.ifClientSecret().isBlank())
                ? request.ifClientSecret()
                : tenant.getIfClientSecret();

        tenant.update(
                request.name(),
                request.platformType(),
                request.apiUrl(),
                request.apiTokenUrl(),
                request.apiClientId(),
                newSecret,
                request.apiCreateDate() != null ? request.apiCreateDate() : tenant.getApiCreateDate(),
                request.ifUrl() != null ? request.ifUrl() : tenant.getIfUrl(),
                request.ifTokenUrl() != null ? request.ifTokenUrl() : tenant.getIfTokenUrl(),
                request.ifClientID(),
                newIfSecret,
                request.ifCreateDate() != null ? request.ifCreateDate() : tenant.getIfCreateDate());
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    @Transactional
    public void delete(Long id) {
        tenantRepository.deleteById(id);
    }

    // syncTenantData()가 여러 REQUIRES_NEW 트랜잭션을 순차적으로 여는 동안 커넥션을 하나씩 열고 닫는데,
    // 여기서 @Transactional을 걸면 그 전체 시간 동안 커넥션 풀에서 커넥션 1개를 추가로 물고 있게 되어
    // 풀 고갈(Could not open JPA EntityManager for transaction)을 유발한다.
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

        // 패키지별 아티팩트 동기화를 순차적으로 실행한다. 동시성을 두면 SAP 호출/REQUIRES_NEW 트랜잭션이
        // 요구하는 JDBC 커넥션 수요를 예측하기 어려워지므로, 단순하고 예측 가능한 순차 처리로 유지한다.
        // syncArtifacts()는 내부적으로 실패를 잡아 skip하므로 여기서 별도 예외 처리는 필요 없다.
        Set<String> activeArtifactIds = new HashSet<>();
        for (IntegrationPackage pkg : editablePackages) {
            artifactSyncService.syncArtifacts(pkg).forEach(
                    artifact -> activeArtifactIds.add(artifact.getSapArtifactId()));
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
