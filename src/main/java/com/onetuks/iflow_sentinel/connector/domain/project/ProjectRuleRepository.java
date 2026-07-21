package com.onetuks.iflow_sentinel.connector.domain.project;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRuleRepository extends JpaRepository<ProjectRule, Long> {

    List<ProjectRule> findByProjectId(Long projectId);

    Optional<ProjectRule> findByProjectIdAndRuleId(Long projectId, Long ruleId);

    List<ProjectRule> findByProjectIdAndIsEnabledTrue(Long projectId);
}
