package com.onetuks.iflow_sentinel.domain.project;

import com.onetuks.iflow_sentinel.domain.rule.Rule;
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
 * ProjectRule 엔티티 클래스
 * 
 * 특정 프로젝트(Project)에서 Rule Library의 특정 규칙(Rule)을 사용할지 말지(ON/OFF)
 * 매핑하고 활성화 여부를 관리하는 엔티티입니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectRule {

    /**
     * 프로젝트 룰 매핑 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 대상 프로젝트
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * 라이브러리에서 가져온 규칙
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    /**
     * 해당 프로젝트에서의 규칙 활성화 여부 (ON/OFF)
     */
    @Column(nullable = false)
    private Boolean isEnabled;

    @Builder
    public ProjectRule(Project project, Rule rule, Boolean isEnabled) {
        this.project = project;
        this.rule = rule;
        this.isEnabled = isEnabled;
    }
}
