package com.onetuks.iflow_sentinel.rule.domain.rule;

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
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;

/**
 * Rule 엔티티 클래스
 * 
 * 시스템 전역 또는 프로젝트 전용으로 사용되는 개별 검사 규칙을 정의합니다.
 * 이 규칙들은 Rule Library(규칙 풀)에 모여 있으며, ProjectRule 매핑을 통해 각 프로젝트에 활성화됩니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rule {

    /**
     * 규칙 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 규칙의 고유 식별 키
     */
    @Column(nullable = false, unique = true)
    private String ruleKey;

    /**
     * 전역(Global) 규칙 여부
     * true: 모든 프로젝트에서 선택 가능한 공통 규칙
     * false: 특정 프로젝트에서만 사용할 목적으로 생성된 커스텀 규칙
     */
    @Column(nullable = false)
    private Boolean isGlobal;

    /**
     * 커스텀 규칙일 경우(isGlobal=false) 해당 규칙을 소유하는 프로젝트
     * 전역 규칙일 경우 null이 됩니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_project_id")
    private Project customProject;

    /**
     * 규칙의 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType type;

    /**
     * 규칙 위반 시의 기본 심각도
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    /**
     * 규칙이 적용될 대상의 정보 (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> target;

    /**
     * 규칙 검사 시 필요한 파라미터 (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> params;

    /**
     * 규칙 위반 시 출력될 메시지 포맷
     */
    private String message;

    /**
     * 라이브러리 레벨에서의 활성화 여부
     * (이 값이 false면 어떤 프로젝트에서도 이 규칙을 활성화할 수 없음)
     */
    @Column(nullable = false)
    private boolean enabled;

    @Builder
    public Rule(
            String ruleKey,
            Boolean isGlobal,
            Project customProject,
            RuleType type,
            Severity severity,
            Map<String, Object> target,
            Map<String, Object> params,
            String message,
            boolean enabled) {
        this.ruleKey = ruleKey;
        this.isGlobal = isGlobal;
        this.customProject = customProject;
        this.type = type;
        this.severity = severity;
        this.target = target;
        this.params = params;
        this.message = message;
        this.enabled = enabled;
    }
}
