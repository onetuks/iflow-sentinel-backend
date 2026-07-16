package com.onetuks.iflow_sentinel.connector.domain.tenant;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Tenant 엔티티 클래스
 * 
 * 특정 프로젝트(Project) 하위에 속하는 대상 시스템 환경(예: DEV, QAS, PRD 테넌트)을 나타냅니다.
 * 테넌트에 연결하기 위한 접속 정보 및 인증 방식 등을 관리합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant {

    /**
     * 테넌트 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 테넌트가 속한 상위 프로젝트
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /**
     * 테넌트 이름 (예: "NanoH2O DEV", "NanoH2O PRD")
     */
    @Column(nullable = false)
    private String name;

    /**
     * 테넌트 연동을 위한 OData 엔드포인트 URL
     */
    @Column(nullable = false)
    private String odataUrl;

    /**
     * 테넌트가 구동되는 플랫폼 타입 (예: NEO, CLOUD_FOUNDRY 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantPlatform platformType;

    /**
     * 테넌트 인증 방식 (예: OAUTH, BASIC 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantAuthType authType;

    /**
     * 인증을 위한 클라이언트 ID
     */
    @Column(nullable = false)
    private String clientId;

    /**
     * 인증을 위한 클라이언트 시크릿 (DB 저장 시 암호화 등 변환 처리 적용)
     */
    @Convert(converter = CredentialConverter.class)
    @Column(nullable = false)
    private String clientSecret;

    @Builder
    public Tenant(
            Project project,
            String name,
            String odataUrl,
            TenantPlatform platformType,
            TenantAuthType authType,
            String clientId,
            String clientSecret) {
        this.project = project;
        this.name = name;
        this.odataUrl = odataUrl;
        this.platformType = platformType;
        this.authType = authType;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
}
