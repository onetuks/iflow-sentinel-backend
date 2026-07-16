package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.dto.TenantCreateRequest;
import com.onetuks.iflow_sentinel.connector.dto.TenantUpdateRequest;
import com.onetuks.iflow_sentinel.connector.persistence.TenantJpaRepository;
import com.onetuks.iflow_sentinel.connector.persistence.ProjectJpaRepository;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantJpaRepository tenantRepository;
    private final ProjectJpaRepository projectRepository;

    @Transactional
    public Tenant createTenant(TenantCreateRequest request) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(NoSuchElementException::new);

        Tenant newTenant = Tenant.builder()
                .project(project)
                .name(request.name())
                .odataUrl(request.odataUrl())
                .platformType(request.platformType())
                .authType(request.authType())
                .clientId(request.clientId())
                .clientSecret(request.clientSecret())
                .build();

        return tenantRepository.save(newTenant);
    }

    @Transactional
    public Tenant updateTenant(Long id, TenantUpdateRequest request) {
        Tenant tenant = tenantRepository.findById(id).orElseThrow(NoSuchElementException::new);
        // Add update logic here if Entity supports it
        return tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant getTenantById(Long id) {
        return tenantRepository.findById(id).orElseThrow(NoSuchElementException::new);
    }

    @Transactional(readOnly = true)
    public Page<Tenant> getTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable);
    }

    @Transactional
    public void removeTenant(Long id) {
        tenantRepository.deleteById(id);
    }
}
