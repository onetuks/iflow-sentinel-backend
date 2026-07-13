package com.onetuks.iflow_sentinel.domain.binding;

import com.onetuks.iflow_sentinel.domain.rule.Rule;
import com.onetuks.iflow_sentinel.domain.rule.Severity;
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
 * BindingOverride 엔티티 클래스
 * 
 * 특정 바인딩(프로젝트-룰셋 매핑)에서 특정 룰에 대한 설정을
 * 프로젝트 종속적으로 재정의(Override)할 때 사용되는 엔티티입니다.
 * 예: 특정 룰의 심각도를 기본값과 다르게 설정하거나 룰을 비활성화할 때 사용됩니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BindingOverride {

    /**
     * 바인딩 오버라이드 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 오버라이드가 속한 바인딩 (어떤 프로젝트-룰셋 매핑에 대한 것인지)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "binding_id", nullable = false)
    private Binding binding;

    /**
     * 재정의할 대상 룰
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    /**
     * 재정의된 심각도 (옵션)
     */
    @Enumerated(EnumType.STRING)
    private Severity overriddenSeverity;

    /**
     * 재정의된 활성화 여부 (옵션)
     */
    private Boolean overriddenEnabled;

    @Builder
    public BindingOverride(Binding binding, Rule rule, Severity overriddenSeverity, Boolean overriddenEnabled) {
        this.binding = binding;
        this.rule = rule;
        this.overriddenSeverity = overriddenSeverity;
        this.overriddenEnabled = overriddenEnabled;
    }
}
