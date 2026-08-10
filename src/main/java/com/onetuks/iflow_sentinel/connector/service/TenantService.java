package com.onetuks.iflow_sentinel.connector.service;

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

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ProjectRepository projectRepository;
    private final TenantConnectionService connectionService;
    private final PackageSyncService packageSyncService;

    public TenantService(TenantRepository tenantRepository, ProjectRepository projectRepository,
            TenantConnectionService connectionService, PackageSyncService packageSyncService) {
        this.tenantRepository = tenantRepository;
        this.projectRepository = projectRepository;
        this.connectionService = connectionService;
        this.packageSyncService = packageSyncService;
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
                .build();

        ConnectionTestResult connectionResult = connectionService.testConnection(tenant);
        if (!connectionResult.success()) {
            throw new IllegalArgumentException("테넌트 연결에 실패하였습니다: " + connectionResult.message());
        }

        Tenant saved = tenantRepository.save(tenant);
        packageSyncService.syncPackages(saved);
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
        Tenant tenant = Tenant.builder()
                .name(request.name())
                .odataUrl(request.odataUrl())
                .tokenUrl(request.tokenUrl())
                .platformType(request.platformType())
                .authType(request.authType())
                .clientId(request.clientId())
                .clientSecret(secret)
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

        tenant.update(
                request.name(),
                request.odataUrl(),
                request.tokenUrl(),
                request.platformType(),
                request.authType(),
                request.clientId(),
                newSecret);
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    @Transactional
    public void delete(Long id) {
        tenantRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    private Tenant findTenant(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("테넌트를 찾을 수 없습니다: " + id));
    }
}
