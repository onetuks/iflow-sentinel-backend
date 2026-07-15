package com.onetuks.iflow_sentinel.report.domain.finding;

import com.onetuks.iflow_sentinel.connector.domain.artifact.Artifact;
import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRun;
import com.onetuks.iflow_sentinel.rule.domain.rule.Rule;
import com.onetuks.iflow_sentinel.rule.domain.rule.Severity;

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
 * Finding 엔티티 클래스
 * 
 * 특정 검사(CheckRun) 과정에서 발견된 규칙 위반 사항이나 결함(이슈)을 의미합니다.
 * 어떤 아티팩트에서 어떤 룰을 위반했는지, 그 심각도와 위치, 상세 메시지 등을 기록합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Finding {

    /**
     * 발견 사항 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 발견 사항이 도출된 검사 실행 이력
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_run_id")
    private CheckRun checkRun;

    /**
     * 규칙을 위반한 대상 아티팩트
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artifact_id", nullable = false)
    private Artifact artifact;

    /**
     * 위반된 규칙
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    /**
     * 발견 사항의 심각도 (예: ERROR, WARNING, INFO 등)
     */
    @Enumerated(EnumType.STRING)
    private Severity severity;

    /**
     * 규칙을 위반한 구체적인 위치 정보 (예: 파일명, 라인 번호, JSON 경로 등)
     */
    private String location;

    /**
     * 위반 사항에 대한 상세 설명 메시지
     */
    private String message;

    @Builder
    public Finding(CheckRun checkRun, Artifact artifact, Rule rule, Severity severity, String location,
            String message) {
        this.checkRun = checkRun;
        this.artifact = artifact;
        this.rule = rule;
        this.severity = severity;
        this.location = location;
        this.message = message;
    }
}
