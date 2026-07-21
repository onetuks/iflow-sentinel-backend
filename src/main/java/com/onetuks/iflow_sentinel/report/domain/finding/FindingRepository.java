package com.onetuks.iflow_sentinel.report.domain.finding;

import com.onetuks.iflow_sentinel.rule.domain.rule.Severity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    List<Finding> findByCheckRunId(Long checkRunId);

    @Query("select f from Finding f where "
            + "(:checkRunId is null or f.checkRun.id = :checkRunId) "
            + "and (:severity is null or f.severity = :severity) "
            + "and (:ruleId is null or f.rule.id = :ruleId) "
            + "and (:artifactId is null or f.artifact.id = :artifactId)")
    List<Finding> search(
            @Param("checkRunId") Long checkRunId,
            @Param("severity") Severity severity,
            @Param("ruleId") Long ruleId,
            @Param("artifactId") Long artifactId);
}
