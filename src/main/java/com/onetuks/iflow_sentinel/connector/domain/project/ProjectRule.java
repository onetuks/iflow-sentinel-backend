package com.onetuks.iflow_sentinel.connector.domain.project;

import com.onetuks.iflow_sentinel.rule.domain.Rule;

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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    private Boolean isEnabled;

    @Builder
    public ProjectRule(Project project, Rule rule, Boolean isEnabled) {
        this.project = project;
        this.rule = rule;
        this.isEnabled = isEnabled;
    }

    public void updateEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}
