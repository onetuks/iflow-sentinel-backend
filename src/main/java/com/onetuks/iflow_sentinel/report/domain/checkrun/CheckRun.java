package com.onetuks.iflow_sentinel.report.domain.checkrun;

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
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;

/**
 * CheckRun 엔티티 클래스
 * 
 * 특정 프로젝트에 대해 실행된 검사(Check)의 이력을 기록합니다.
 * 검사의 시작 시간, 상태, 요약 결과 등을 저장합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckRun {

    /**
     * 검사 실행 이력 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 검사를 수행한 대상 프로젝트
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * 검사 시작 일시
     */
    @Column
    private LocalDateTime startedAt;

    /**
     * 검사 진행 상태 (예: RUNNING, COMPLETED, FAILED)
     */
    @Enumerated(EnumType.STRING)
    private CheckRunStatus status;

    /**
     * 검사 결과 요약(통계)을 저장하는 JSON 데이터
     */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> summary;

    @Builder
    public CheckRun(Project project, LocalDateTime startedAt, CheckRunStatus status) {
        this.project = project;
        this.startedAt = startedAt;
        this.status = status;
    }

    public void updateStatus(CheckRunStatus status, Map<String, Object> summary) {
        this.status = status;
        this.summary = summary;
    }
}
