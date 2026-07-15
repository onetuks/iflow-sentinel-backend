package com.onetuks.iflow_sentinel.connector.domain.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Project 엔티티 클래스
 * 
 * 시스템 내에서 관리되는 최상위 단위인 '프로젝트'를 나타냅니다.
 * 하나의 프로젝트 하위에 여러 개의 테넌트(Tenant)가 속할 수 있으며,
 * 프로젝트 단위로 검사 규칙(ProjectRule)이 관리됩니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    /**
     * 프로젝트 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 프로젝트 이름 (예: "NanoH2O IS 전환 프로젝트")
     */
    @Column(nullable = false)
    private String name;

    @Builder
    public Project(String name) {
        this.name = name;
    }

    public Project setName(String newName) {
        this.name = newName;
        return this;
    }
}
