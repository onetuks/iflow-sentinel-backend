package com.onetuks.iflow_sentinel.connector.domain.integrationpackage;

import com.onetuks.iflow_sentinel.domain.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * IntegrationPackage 엔티티 클래스
 * 
 * 특정 테넌트(Tenant)에 배포되어 있는 SAP CPI(Cloud Platform Integration) 등의
 * 통합 패키지(Integration Package) 단위 정보를 관리합니다.
 * 이 패키지 하위에 실제 개별 iFlow(Artifact)들이 포함됩니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IntegrationPackage {

    /**
     * 패키지 내부 관리 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 패키지가 속해 있는 테넌트
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    /**
     * 원격 SAP 시스템에서 관리되는 패키지의 고유 ID
     */
    @Column(nullable = false, unique = true)
    private String sapPackageId;

    /**
     * 패키지 이름
     */
    @Column(nullable = false)
    private String name;

    @Builder
    public IntegrationPackage(Tenant tenant, String sapPackageId, String name) {
        this.tenant = tenant;
        this.sapPackageId = sapPackageId;
        this.name = name;
    }
}
