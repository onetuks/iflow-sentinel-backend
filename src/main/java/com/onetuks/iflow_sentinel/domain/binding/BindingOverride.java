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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BindingOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "binding_id")
    private Binding binding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    @Enumerated(EnumType.STRING)
    private Severity overriddenSeverity;

    private Boolean overriddenEnabled;

    @Builder
    public BindingOverride(Rule rule, Severity overriddenSeverity, Boolean overriddenEnabled) {
        this.rule = rule;
        this.overriddenSeverity = overriddenSeverity;
        this.overriddenEnabled = overriddenEnabled;
    }

    public void assignBinding(Binding binding) {
        this.binding = binding;
    }
}
