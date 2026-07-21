package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;
import com.onetuks.iflow_sentinel.connector.dto.ConnectionTestResult;
import com.onetuks.iflow_sentinel.connector.dto.TenantRequest;
import com.onetuks.iflow_sentinel.connector.dto.TenantResponse;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ProjectRepository projectRepository;
    private final TenantConnectionService connectionService;

    public TenantService(TenantRepository tenantRepository, ProjectRepository projectRepository,
            TenantConnectionService connectionService) {
        this.tenantRepository = tenantRepository;
        this.projectRepository = projectRepository;
        this.connectionService = connectionService;
    }

    public TenantResponse create(TenantRequest request) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new NoSuchElementException("프로젝트를 찾을 수 없습니다: " + request.projectId()));

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
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    public List<TenantResponse> list(Long projectId) {
        List<Tenant> tenants = projectId != null ? tenantRepository.findByProjectId(projectId)
                : tenantRepository.findAll();
        return tenants.stream().map(TenantResponse::from).toList();
    }

    public TenantResponse get(Long id) {
        return TenantResponse.from(findTenant(id));
    }

    public ConnectionTestResult testConnection(Long id) {
        return connectionService.testConnection(findTenant(id));
    }

    public TenantResponse update(Long id, TenantRequest request) {
        Tenant tenant = findTenant(id);
        tenant.update(
                request.name(),
                request.odataUrl(),
                request.tokenUrl(),
                request.platformType(),
                request.authType(),
                request.clientId(),
                request.clientSecret());
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    public void delete(Long id) {
        tenantRepository.deleteById(id);
    }

    private Tenant findTenant(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("테넌트를 찾을 수 없습니다: " + id));
    }
}
