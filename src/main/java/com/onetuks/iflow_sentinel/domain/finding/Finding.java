package com.onetuks.iflow_sentinel.domain.finding;

import com.onetuks.iflow_sentinel.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.domain.checkrun.CheckRun;
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
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_run_id")
    private CheckRun checkRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artifact_id", nullable = false)
    private Artifact artifact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private String location;

    private String message;

    @Builder
    public Finding(CheckRun checkRun, Artifact artifact, Rule rule, Severity severity, String location, String message) {
        this.checkRun = checkRun;
        this.artifact = artifact;
        this.rule = rule;
        this.severity = severity;
        this.location = location;
        this.message = message;
    }
}
