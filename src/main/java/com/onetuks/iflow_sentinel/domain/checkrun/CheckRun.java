package com.onetuks.iflow_sentinel.domain.checkrun;

import com.onetuks.iflow_sentinel.domain.finding.Finding;
import com.onetuks.iflow_sentinel.domain.project.Project;
import com.onetuks.iflow_sentinel.domain.ruleset.Ruleset;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruleset_id", nullable = false)
    private Ruleset ruleset;

    @Column
    private LocalDateTime startedAt;

    @Enumerated(EnumType.STRING)
    private CheckRunStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> summary;

    @OneToMany(mappedBy = "checkRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Finding> findings = new ArrayList<>();

    @Builder
    public CheckRun(Project project, Ruleset ruleset, LocalDateTime startedAt, CheckRunStatus status) {
        this.project = project;
        this.ruleset = ruleset;
        this.startedAt = startedAt;
        this.status = status;
    }

    public void addFinding(Finding finding) {
        findings.add(finding);
        finding.assignCheckRun(this);
    }

    public void updateStatus(CheckRunStatus status, Map<String, Object> summary) {
        this.status = status;
        this.summary = summary;
    }
}
