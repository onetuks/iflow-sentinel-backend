package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.artifact.ArtifactRepository;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.domain.tenant.TenantRepository;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * 아티팩트 추적 화면에서의 Deploy/Undeploy/design-time 삭제 액션. SAP 아티팩트 ID를 기준으로 동작하므로
 * runtime에만 존재하는(INACTIVE, 로컬 DB에 대응 레코드가 없는) 아티팩트에도 적용할 수 있다.
 */
@Service
public class ArtifactTrackerActionService {

    private final TenantRepository tenantRepository;
    private final ArtifactRepository artifactRepository;
    private final SapODataClient odataClient;

    public ArtifactTrackerActionService(TenantRepository tenantRepository, ArtifactRepository artifactRepository,
            SapODataClient odataClient) {
        this.tenantRepository = tenantRepository;
        this.artifactRepository = artifactRepository;
        this.odataClient = odataClient;
    }

    /** 지정한 design-time 아티팩트의 활성(active) 버전을 배포한다. */
    public void deploy(Long tenantId, String artifactId) {
        Tenant tenant = findTenant(tenantId);
        String relativePath = "/DeployIntegrationDesigntimeArtifact?Id='" + artifactId + "'&Version='active'";
        odataClient.executeAction(tenant, HttpMethod.POST, relativePath);
    }

    /** 배포된 runtime 아티팩트를 undeploy 한다. */
    public void undeploy(Long tenantId, String artifactId) {
        Tenant tenant = findTenant(tenantId);
        String relativePath = "/IntegrationRuntimeArtifacts('" + artifactId + "')";
        odataClient.executeAction(tenant, HttpMethod.DELETE, relativePath);
    }

    /** design-time 아티팩트를 삭제한다. 로컬에 동기화된 레코드가 있으면 함께 제거한다. */
    public void deleteDesigntimeArtifact(Long tenantId, String artifactId, String version) {
        Tenant tenant = findTenant(tenantId);
        String relativePath = "/IntegrationDesigntimeArtifacts(Id='" + artifactId + "',Version='" + version + "')";
        odataClient.executeAction(tenant, HttpMethod.DELETE, relativePath);
        artifactRepository.findBySapArtifactId(artifactId).ifPresent(artifactRepository::delete);
    }

    private Tenant findTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("테넌트를 찾을 수 없습니다: " + tenantId));
    }
}
