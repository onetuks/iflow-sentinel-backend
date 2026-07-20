package com.onetuks.iflow_sentinel.domain.binding;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.domain.ruleset.Ruleset;
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
 * Binding 엔티티 클래스
 * 
 * 프로젝트(Project)와 룰셋(Ruleset) 간의 매핑 정보를 관리합니다.
 * 특정 프로젝트가 어떤 룰셋 정책들을 적용하고 있는지 나타내는 연결 고리 역할을 합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Binding {

    /**
     * 바인딩 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 바인딩이 적용되는 프로젝트
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * 프로젝트에 적용할 룰셋
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruleset_id", nullable = false)
    private Ruleset ruleset;

    @Builder
    public Binding(Project project, Ruleset ruleset) {
        this.project = project;
        this.ruleset = ruleset;
    }
}
