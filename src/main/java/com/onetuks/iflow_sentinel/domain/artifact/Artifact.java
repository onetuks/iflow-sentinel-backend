package com.onetuks.iflow_sentinel.domain.artifact;

import com.onetuks.iflow_sentinel.domain.integrationpackage.IntegrationPackage;
import jakarta.persistence.Column;
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
 * Artifact 엔티티 클래스
 * 
 * 통합 패키지(IntegrationPackage) 내부에 포함된 개별 통합 자산(iFlow, Value Mapping 등)을 나타냅니다.
 * 이 아티팩트 단위로 규칙 검사(CheckRun)가 수행되어 결함(Finding)이 도출됩니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artifact {

    /**
     * 아티팩트 내부 관리 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 아티팩트가 속해 있는 상위 통합 패키지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private IntegrationPackage integrationPackage;

    /**
     * 원격 SAP 시스템에서 관리되는 아티팩트의 고유 ID
     */
    @Column(nullable = false)
    private String sapArtifactId;

    /**
     * 아티팩트 이름
     */
    @Column(nullable = false)
    private String name;

    /**
     * 아티팩트의 현재 버전 정보
     */
    @Column(nullable = false)
    private String version;

    /**
     * 아티팩트의 종류 (예: IFLOW, VALUE_MAPPING 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArtifactType type;

    @Builder
    public Artifact(IntegrationPackage integrationPackage, String sapArtifactId, String name, String version, ArtifactType type) {
        this.integrationPackage = integrationPackage;
        this.sapArtifactId = sapArtifactId;
        this.name = name;
        this.version = version;
        this.type = type;
    }
}
